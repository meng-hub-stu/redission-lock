package com.mengdx.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static java.util.Arrays.stream;

/**
 * @author Mengdl
 * @date 2022/06/27
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum implements IStatus {
    /**
     * 发送的消息类型
     */
    LIST(1, "list类型的消息"),
    SET(2, "set类型的消息"),
    QUEUE(3, "发布订阅的模式"),
    STREAM(4, "流方式");

    private final Integer code;
    private final String desc;

    public static MessageTypeEnum of(String desc) {
        return stream(values()).filter(type -> type.desc.equals(desc)).findFirst().orElse(null);
    }

}
