package com.mengdx.aop;

import cn.hutool.core.util.StrUtil;
import com.mengdx.annotation.RateLimiter;
import com.mengdx.entity.enums.RateLimiterTypeEnum;
import com.mengdx.exception.RedisRuntimeException;
import com.mengdx.utils.AspectUtil;
import com.mengdx.utils.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解切面（通过Redis Lua脚本实现限流）
 *
 * @author Mengdl
 * @date 2020/10/16 2:47 下午
 */
@Aspect
@Component
@Slf4j
@Order(11)
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final static String SEPARATOR = ":";
    private final static String REDIS_LIMIT_KEY_PREFIX = "limit";
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> limitRedisScript;

    @Pointcut("@annotation(com.mengdx.annotation.RateLimiter)")
    private void rateLimit() {
    }
    
    @Around("rateLimit()")
    public Object redisRepeat(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 通过 AnnotationUtils.findAnnotation 获取 RateLimiter 注解
        RateLimiter rateLimiter = AnnotationUtils.findAnnotation(method, RateLimiter.class);
        if (rateLimiter != null) {
            // limit:类名:方法名
            String key = REDIS_LIMIT_KEY_PREFIX
                    + SEPARATOR
                    + StrUtil.subAfter(method.getDeclaringClass().getName(), ".", true)
                    + SEPARATOR
                    + method.getName();

            if (RateLimiterTypeEnum.IP.equals(rateLimiter.type())) {
                // limit:类名:方法名:ip
                key = key + SEPARATOR + IpUtil.getIpAddr();
            } else {
                // limit:类名:方法名:参数哈希Code
                key = key + SEPARATOR + Objects.hash(AspectUtil.getParams(point));
            }

            // 执行限流方法，判断是否应该限流
//            Assert.isTrue(shouldLimited(key, rateLimiter.max(), rateLimiter.timeout(), rateLimiter.timeUnit()), "操作过于频繁，请稍后再试");
            if (!shouldLimited(key, rateLimiter.max(), rateLimiter.timeout(), rateLimiter.timeUnit())) {
                throw new RedisRuntimeException("操作过于频繁，请稍后再试");
            }
        }

        return point.proceed();

    }

    private boolean shouldLimited(String key, long max, long timeout, TimeUnit timeUnit) {
        // 统一使用单位毫秒
        long ttl = timeUnit.toMillis(timeout);
        // 当前时间毫秒数
        long now = Instant.now().toEpochMilli();
        long expired = now - ttl;
        // 注意这里必须转为 String，否则会报错 java.lang.Long cannot be cast to java.lang.String
        Long executeTimes = stringRedisTemplate.execute(limitRedisScript, Collections.singletonList(key), now + "", ttl + "", expired + "", max + "");
        if (executeTimes != null) {
            if (executeTimes == 0) {
                log.warn("【{}】在单位时间 {} 秒内已达到访问上限，当前接口上限 {}", key, ttl / 1000, max);
                return false;
            } else {
                log.info("【{}】在单位时间内 {} 秒内访问 {} 次", key, ttl / 1000, executeTimes);
                return true;
            }
        }
        return true;
    }

}
