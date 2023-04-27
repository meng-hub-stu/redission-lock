package com.mengdx.aop;

import com.mengdx.annotation.RedisLock;
import com.mengdx.entity.enums.LockModel;
import com.mengdx.exception.RedisRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static jodd.util.StringPool.COLON;
import static jodd.util.StringPool.DOT;

/**
 * @author Mengdl
 * @date 2022/01/18
 */
@Aspect
@Component
@Slf4j
@Order(10)
public class RedisLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(redisLock)")
    public Object lockAroundAction(ProceedingJoinPoint point, RedisLock redisLock) throws Throwable {
        //获取参数
        Object[] parameterValues = point.getArgs();
        //获取方法的签名
        MethodSignature methodSignature = (MethodSignature)point.getSignature();
        //获取类名
        String className = point.getTarget().getClass().getName();
        //获取注解中的参数
        int retryNum = redisLock.retryNum();
        long retryWait = redisLock.retryWait();
        long lockWait = redisLock.lockWait();
        long autoUnlockTime = redisLock.autoUnlockTime();
        String lockPrefix = redisLock.lockPrefix();
        if (StringUtils.isBlank(lockPrefix)) {
            lockPrefix = className + DOT + methodSignature.getName();
        }
        //获取参数的el数据
        String el = parseByEl(redisLock.lockParameter(), methodSignature.getParameterNames(), parameterValues);
        //设置锁的名称
        String lockName = lockPrefix;
        if (StringUtils.isNotBlank(redisLock.lockParameter())){
            lockName = lockPrefix + COLON + el;
        }
        RLock lock = this.getLockByModel(redisLock.lockModel(), lockPrefix, el);
        try {
            //使用tryLock加锁，不要使用lock
            boolean res = lock.tryLock(lockWait, autoUnlockTime, TimeUnit.MILLISECONDS);
            if (res) {
                log.info(String.format("线程：%s->获得锁，锁的名称为：%s", Thread.currentThread().getName(), lockName));
                return point.proceed();
            } else {
                // 如果重试次数为零, 则不重试
                if (retryNum <= 0) {
                    log.info(String.format("{%s}已经被锁, 不重试", lockName));
                    throw new RedisRuntimeException(String.format("{%s}已经被锁, 不重试", lockName));
                }
                //重试等待时间
                if (retryWait == 0) {
                    retryWait = 200L;
                }
                // 设置失败次数计数器, 当到达指定次数时, 返回失败
                int failCount = 1;
                while (failCount <= retryNum) {
                    // 等待指定时间ms
                    Thread.sleep(retryWait);
                    if (lock.tryLock(lockWait, autoUnlockTime, TimeUnit.MILLISECONDS)) {
                        // 执行主逻辑
                        return point.proceed();
                    } else {
                        log.info(String.format("{%s}已经被锁, 正在重试[ %s/%s ],重试间隔{%s}毫秒", lockName, failCount, retryNum,
                                retryWait));
                        failCount++;
                    }
                }
                throw new RedisRuntimeException("系统繁忙, 请稍等再试");
            }
        } catch (Throwable throwable) {
            log.error(String.format("执行分布式锁发生异常锁名:{%s},异常名称:{%s}", lockName, throwable.getMessage()));
            throw throwable;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取锁
     * @param lockModel 锁的类型
     * @param lockPrefix 锁的名称
     * @param el 参数类型
     * @return 返回锁
     */
    public RLock getLockByModel(LockModel lockModel, String lockPrefix, String el){
        String lockName = StringUtils.isNotBlank(el) ? lockPrefix + COLON + el : lockPrefix;
        switch (lockModel) {
            case _REENTRANT:
                return redissonClient.getLock(lockName);
            case _FAIR:
                return redissonClient.getFairLock(lockName);
            case _READ:
                return redissonClient.getReadWriteLock(lockName).readLock();
            case _WRITE:
                return redissonClient.getReadWriteLock(lockName).writeLock();
            case _RED_LOCK:
                return new RedissonRedLock(StringUtils.isNotBlank(el) ?
                        Arrays.stream(el.split(COLON)).map(v -> redissonClient.getLock(lockPrefix + COLON + v))
                                .collect(Collectors.toList()).toArray(new RLock[el.split(COLON).length])
                        : new RLock[]{redissonClient.getLock(lockName)});
            case _MULTIPLE:
                return redissonClient.getMultiLock(StringUtils.isNotBlank(el) ?
                        Arrays.stream(el.split(COLON)).map(v -> redissonClient.getLock(lockPrefix + COLON + v))
                                .collect(Collectors.toList()).toArray(new RLock[el.split(COLON).length])
                        : new RLock[]{redissonClient.getLock(lockName)});
            default:
                throw new RedisRuntimeException("参数异常");
        }
    }

    /**
     * 解析el表达式
     * @param key elc参数
     * @param parameterNames 参数名称
     * @param parameterValues 参数值
     * @return 返回解析的数据
     */
    public static String parseByEl(String key, String[] parameterNames, Object[] parameterValues) {
        if (StringUtils.isBlank(key)) {
            return key;
        }
        Expression expression = new SpelExpressionParser().parseExpression(key);
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (ArrayUtils.isEmpty(parameterValues) || ArrayUtils.isEmpty(parameterNames)) {
            return key;
        }
        for (int i = 0; i < parameterValues.length; i++) {
            context.setVariable(parameterNames[i], parameterValues[i]);
        }
        try {
            Object expressionValue = expression.getValue(context);
            if (Objects.nonNull(expressionValue) && StringUtils.isNotBlank(expressionValue.toString())) {
                return expressionValue.toString();
            }else{
                return key;
            }
        } catch (Exception e) {
            return key;
        }
    }

}
