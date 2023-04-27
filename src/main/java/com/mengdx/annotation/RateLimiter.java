package com.mengdx.annotation;

import com.mengdx.entity.enums.RateLimiterTypeEnum;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流
 * @author Mengdl
 * @date 2022/06/16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Order(value = 11)
public @interface RateLimiter {

    long DEFAULT_MAX_REQUEST = 1;

    /**
     * 限流类型 {@link RateLimiterTypeEnum}，默认通过请求参数限流
     */
    RateLimiterTypeEnum type() default RateLimiterTypeEnum.PARAM;

    /**
     * max 最大请求数, 默认1
     */
    @AliasFor("value") long max() default DEFAULT_MAX_REQUEST;

    /**
     * max 最大请求数, 默认1
     */
    @AliasFor("max") long value() default DEFAULT_MAX_REQUEST;

    /**
     * 超时时长，默认 3 秒
     */
    long timeout() default 3;

    /**
     * 超时时间单位，默认 秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
