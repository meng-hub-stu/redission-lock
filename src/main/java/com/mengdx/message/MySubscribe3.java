package com.mengdx.message;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.util.Map;

/**
 * queue队列的监听者
 * @author Mengdl
 * @date 2022/07/07
 */
public class MySubscribe3 implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] bytes) {
        Jackson2JsonRedisSerializer<Map> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Map.class);
        Map<String, Object> user = jacksonSerializer.deserialize(message.getBody());
        System.out.println("订阅频道3:" + new String(message.getChannel()));
        System.out.println("接收数据3:" + user);
    }
    
}
