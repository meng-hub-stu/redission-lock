package com.mengdx.message;

import com.alibaba.fastjson.JSON;
import com.mengdx.entity.RedisMessage;
import com.mengdx.entity.enums.IStatus;
import com.mengdx.entity.enums.MessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * redis实现消息队列
 * @author Mengdl
 * @date 2022/06/27
 */
@Component
@Slf4j
public class RedisMessageProduct<T> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 发送消息
     * @param message 消息内容
     */
    public void redisSendMessage(RedisMessage<T> message) {
        IStatus.find(message.getMessageType().getCode(), MessageTypeEnum.class).orElseThrow(() -> new RuntimeException("不存在的消息类型"));
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.execute(() -> {
            switch (message.getMessageType()) {
                case LIST:
                    pushListMessage(message);
                    return ;
                case SET:
                    pushSetMessage(message);
                    return ;
                case QUEUE:
                    pushQueueMessage(message);
                    return ;
                case STREAM:
                    pushStreamMessage(message);
                    return ;
                default:
                    return ;
            }
        });
    }

    /**
     * list发送消息内容
     * @param message 消息体
     * @return
     */
    private boolean pushListMessage(RedisMessage<T> message) {
        ListOperations<String, Object> opsForList = redisTemplate.opsForList();
        opsForList.leftPush(message.getKey().toString(), JSON.toJSONString(message.getData()));
        log.info("发送list方式的消息成功");
        return true;
    }

    /**
     * 发送set消息
     * @param message 消息体
     * @return 返回结果
     */
    private boolean pushSetMessage(RedisMessage<T> message) {
        ZSetOperations<String, Object> opsForZSet = redisTemplate.opsForZSet();
        redisTemplate.convertAndSend(message.getTopic(), message.getData());
        log.info("发送set方式的消息成功");
        return true;
    }

    /**
     * 发送订阅消息
     * @param message 消息体
     * @return 返回结果
     */
    private boolean pushQueueMessage(RedisMessage<T> message) {
        redisTemplate.convertAndSend(message.getTopic(), message.getData());
        log.info("发送订阅方式的消息成功");
        return true;
    }

    private boolean pushStreamMessage(RedisMessage<T> message) {
        redisTemplate.convertAndSend(message.getTopic(), message.getData());
        return true;
    }


}
