package com.mengdx.entity.enums;

/**
 * 限流类型枚举
 *
 * @author Mengdl
 * @date 2020/10/16 3:00 下午
 */
public enum RateLimiterTypeEnum {

    /**
     * 通过IP限流
     */
    IP,

    /**
     * 通过请求参数限流
     */
    PARAM

}
