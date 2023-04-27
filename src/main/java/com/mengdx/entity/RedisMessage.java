package com.mengdx.entity;

import com.mengdx.entity.enums.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static com.mengdx.entity.enums.MessageTypeEnum.LIST;

/**
 * 自定义redis消息载体
 * @author Mengdl
 * @date 2022/06/27
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RedisMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主题
     */
    private String topic;

    /**
     * 消息的key
     */
    private T key;

    /**
     * 消息内容
     */
    private T data;

    /**
     * 消息推送的方式（默认使用list的方式）
     */
    private MessageTypeEnum messageType = LIST;

}
