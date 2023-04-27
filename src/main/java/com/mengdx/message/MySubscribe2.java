package com.mengdx.message;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * queue队列的监听者
 * @author Mengdl
 * @date 2022/07/07
 */
public class MySubscribe2 implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] bytes) {
        System.out.println("订阅频道2:" + new String(message.getChannel()));
        System.out.println("接收数据2:" + new String(message.getBody()));
    }
    
}
