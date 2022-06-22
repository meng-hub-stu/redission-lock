package com.mengdx.cache;

import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * @author Mengdl
 * @date 2022/06/20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Order(value = 10)
@Repeatable(value = CascadeEvictKeys.class)
public @interface CascadeEvictKey {
    /**
     * 缓存名称，方法存在多个CacheEvict时要指定
     * @return 缓存名称
     */
    String cacheName() default "";

    String[] value() default {};

}
