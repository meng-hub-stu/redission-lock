package com.mengdx.annotation;

import com.mengdx.entity.enums.LockModel;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

import static com.mengdx.entity.enums.LockModel._REENTRANT;

/**
 * 分布式锁注解
 * @author Mengdl
 * @date 2022/06/16
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Order(value = 10)
public @interface RedisLock {
    /**
     * 锁前缀 为空则取类名加方法名的拼接
     * @return keys值前缀
     */
    String lockPrefix() default "";

    /**
     * el表达式
     * @return el的keys值值
     */
    String lockParameter() default "";

    /**
     * 加锁等待时间(毫秒)
     * @return 等待锁时间
     */
    long lockWait() default 3000L;

    /**
     * 自动解锁时间 （毫秒）
     * @return 默认10000毫秒
     */
    long autoUnlockTime() default 10000L;

    /**
     * 重试次数
     * @return 默认0次
     */
    int retryNum() default 0;

    /**
     * 重试等待时间
     * @return 默认500毫秒
     */
    long retryWait() default 500L;

    /**
     * 获取锁的类型
     * @return 默认是可重入锁
     */
    LockModel lockModel() default _REENTRANT;

}
