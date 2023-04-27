package com.mengdx.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.types.Expiration;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 此方式，我是用在了限流中，其实也可以使用进行编写分布式锁
 * 用到lua脚本
 * @author Mengdl
 * @date 2022/06/27
 */
@Slf4j
public class RedisLockUtil implements AutoCloseable {

    private RedisTemplate redisTemplate;
    private String key;
    private String value;
    private Integer expiration;

    public RedisLockUtil(RedisTemplate redisTemplate, String key, Integer expiration) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.expiration = expiration;
        this.value = "123".toString();
    }

    /**
     * 获取分布式锁
     * @return 返回结果
     */
    public boolean getLock(){
        RedisCallback<Boolean> redisCallback = connection -> {
            //设置NX
            RedisStringCommands.SetOption setOption = RedisStringCommands.SetOption.ifAbsent();
            //设置过期时间
            Expiration expiration = Expiration.milliseconds(this.expiration);
            //序列化key
            byte[] redisKey = redisTemplate.getKeySerializer().serialize(this.key);
            byte[] redisValue = redisTemplate.getValueSerializer().serialize(this.value);
            //执行setnx操作
            return connection.set(redisKey, redisValue, expiration, setOption);
        };
        //获得锁
        Boolean result = (Boolean) redisTemplate.execute(redisCallback);
        log.info("获得锁结果：{}", result);
        return result;
    }

    /**
     * 关闭分布式锁
     * @return 返回结果
     */
    public boolean unLock(){
        String script = "if redis.call(\"get\",KEYS[1] == ARGV[1] then\n" +
                " return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                " return 0\n" +
                "end)";
        List<String> keys = Arrays.asList(key);
        RedisScript<Boolean> redisScript = RedisScript.of(script, Boolean.class);
        Boolean result = (Boolean)redisTemplate.execute(redisScript, keys, value);
        log.info("释放锁的结果：{}", result);
        return result;
    }

    @Override
    public void close() throws Exception {
        unLock();
    }

}
