package com.mengdx.cache;

import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * 清除级联的缓存
 * @author Mengdl
 * @date 2022/06/20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Order(value = 10)
public @interface CascadeEvictKeys {

    CascadeEvictKey[] value();

}
