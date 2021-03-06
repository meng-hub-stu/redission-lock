package com.mengdx.config;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mengdx.cache.CacheKey;
import com.mengdx.cache.CascadeEvictKey;
import com.mengdx.cache.CascadeKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;

/**
 * @author Mengdl
 * @date 2022/06/17
 */
@Configuration
@ComponentScan({"com.mengdx"})
@Slf4j
public class RedissionConfiguration extends CachingConfigurerSupport {

    //???????????????????????????????????????????????????
    private static String tenantId = "000000";

    @Bean
    public KeyGenerator orderKeyGenerator() {
        return (o, method, objects) -> {
            return "order";
        };
    }

    /**
     * ??????????????????cache???key
     * @return ??????????????????
     */
    @Override
    @Bean
    public KeyGenerator keyGenerator() {
        return (o, method, objects) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(o.getClass().getName()).append(".");
            sb.append(method.getName()).append(".");
            for (Object obj : objects) {
                sb.append(obj.toString());
            }
            System.out.println("keyGenerator=" + sb.toString());
            return sb.toString();
        };
    }

    /**
     * ??????key????????????????????????+????????????
     *
     * @return ??????key
     */
    @Bean
    TenantCacheKeyGenerator tenantKeyGenerator() {
        TenantCacheKeyGenerator generator = new TenantCacheKeyGenerator();
        generator.setIncludeParams(true);
        return generator;
    }

    /**
     * ??????key???????????????????????????+???????????????????????????????????????????????????????????????
     * ??????????????????????????????????????????????????????????????????CascadeEvictKey???????????????
     *
     * @return ??????key
     */
    @Bean
    TenantCacheKeyGenerator tenantPrimaryKeyGenerator() {
        TenantCacheKeyGenerator generator = new TenantCacheKeyGenerator();
        generator.setIncludePrimaryKey(true);
        generator.setIncludeCascadeKeys(true);
        return generator;
    }

    /**
     * ??????key????????????????????????????????????????????????CascadeEvictKey???????????????
     *
     * @return ??????key
     */
    @Bean
    TenantCacheKeyGenerator tenantCascadeKeyGenerator() {
        TenantCacheKeyGenerator generator = new TenantCacheKeyGenerator();
        generator.setIncludeCascadeKeys(true);
        return generator;
    }

    /**
     * ??????key???????????????????????????+?????????+??????????????????????????????
     *
     * @return ??????key
     */
    @Bean
    TenantCacheKeyGenerator tenantEntireKeyGenerator() {
        return new TenantCacheKeyGenerator(true, true, true);
    }

    /**
     * ???????????????key???????????????
     */
    @Data
    @RequiredArgsConstructor
    @AllArgsConstructor
    @Slf4j
    private static class TenantCacheKeyGenerator implements KeyGenerator {
        /**
         * ?????????????????????????????????????????????
         */
        private boolean includePrimaryKey = false;
        /**
         * ????????????
         */
        private boolean includeParams = false;
        /**
         * ???????????????
         */
        private boolean includeCascadeKeys = false;

        @Override
        public Object generate(Object target, Method method, Object... params) {
            Object primaryKey = includePrimaryKey && params.length > 0 ? params[0] : null;
            if (includePrimaryKey && ObjectUtil.isEmpty(primaryKey)) {
                log.warn("?????? {}?????????????????????????????????", method.getName());
            }
            String paramsKey = includeParams && ArrayUtil.isNotEmpty(params) ? arrayToCommaDelimitedString(params) : null;
            List<Object> keys = new ArrayList<>();
            keys.add(tenantId);
            if (ObjectUtil.isNotEmpty(primaryKey)) {
                keys.add(primaryKey);
            }
            keys.add(method.getName());
            if (ObjectUtil.isNotEmpty(paramsKey)) {
                keys.add(paramsKey);
            }

            return new CacheKey(
                    keys.stream().map(Object::toString).collect(Collectors.joining(":")),
                    includeCascadeKeys ? getAnnotation(primaryKey, target, method, params) : new ArrayList<>()
            );
        }

        private static List<CascadeKey> getAnnotation(Object primaryKey, Object target, Method method, Object... params) {
            CascadeEvictKey[] cascadeEvictKeys = method.getAnnotationsByType(CascadeEvictKey.class);
            if (ArrayUtil.isEmpty(cascadeEvictKeys)) {
                cascadeEvictKeys = target.getClass().getAnnotationsByType(CascadeEvictKey.class);
            }
            List<CascadeKey> keys = new ArrayList<>();
            if (ArrayUtil.isNotEmpty(cascadeEvictKeys)) {
                for (CascadeEvictKey key : cascadeEvictKeys) {
                    keys.add(new CascadeKey(key.cacheName(), Arrays.stream(key.value())
                            .map(k -> parsingParams(k, method, params)).filter(CharSequenceUtil::isNotBlank).toArray(String[]::new)));
                }
            }
            if (ObjectUtil.isNotEmpty(primaryKey)) {
                keys.add(new CascadeKey("", new String[]{primaryKey + ":"}));
            }
            return keys;
        }

        @SneakyThrows
        private static String parsingParams(String key, Method method, Object... params) {
            // ????????????key?????????.?????????????????????????????????
            String dot = ".";
            if (key.contains(dot)) {
                Parameter[] parameters = method.getParameters();
                String[] strs = key.split("\\.");
                int length = 2;
                if (strs.length == length) {
                    for (int i = 0; i < parameters.length; i++) {
                        if (strs[0].equals(parameters[i].getName())) {
                            Object param = params[i];
                            Object value = param.getClass().getMethod("get" + StrUtil.upperFirst(strs[1])).invoke(param);
                            return null != value ? tenantId + ":" + value.toString() + ":" : null;
                        }
                    }
                }
            }
            return tenantId + ":" + key;
        }
    }



    /**
     * redis?????????????????????
     * @param factory ????????????
     * @return ??????redisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setConnectionFactory(factory);
        //key???????????????
        template.setKeySerializer(redisSerializer);
        //value?????????
        template.setValueSerializer(jackson2JsonRedisSerializer);
        //value hashmap?????????
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }

    /**
     * ??????redis???????????????
     * @param factory redis????????????
     * @return ????????????
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        //???????????????????????????????????????
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // ??????????????????????????????????????????,????????????600???
        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(600))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();

        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // ???????????????cacheName?????????RedisCacheConfiguration  ???????????????????????????
                //.withCacheConfiguration("Users",config.entryTtl(Duration.ofSeconds(100)))
                .transactionAware()
                .build();
        return cacheManager;
    }

    /**
     * ????????????
     */
    @Bean
    public RedisScript<Long> limitRedisScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts.redis/limit.lua")));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

}
