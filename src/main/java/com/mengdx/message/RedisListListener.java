package com.mengdx.message;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 监听list的数据
 * @author Mengdl
 * @date 2022/06/27
 */
@Slf4j
//@Component
//@EnableScheduling
@AllArgsConstructor
public class RedisListListener {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY = "redis-list";

    //    @Scheduled(initialDelay = 5 * 1000, fixedRate = 2 * 1000)
    public void onMessage() {
        log.info("定时器5秒执行一次消费消息->{}", "redis-list");
        String rightPop = (String)redisTemplate.opsForList().rightPop(KEY);
        log.info("消息内容->{}", rightPop);
        if (isNotBlank(rightPop)) {
            Map<String, Object> map = JSON.parseObject(rightPop.toString(), Map.class);
            System.out.println(map);
        }
    }

    @PostConstruct
    public void brPop() {
        new Thread(() -> {
            while (true) {
                String message = (String) redisTemplate.opsForList().rightPop(KEY, 10, TimeUnit.SECONDS);
                log.info("消息1" + message);
            }
        }).start();
    }

    @PostConstruct
    public void b1rPop() {
        new Thread(() -> {
            while (true) {
                String message = (String) redisTemplate.opsForList().rightPop(KEY, 10, TimeUnit.SECONDS);
                log.info("消息2"+ message);
            }
        }).start();
    }

}
