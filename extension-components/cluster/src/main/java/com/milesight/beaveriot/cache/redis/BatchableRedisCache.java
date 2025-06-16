package com.milesight.beaveriot.cache.redis;

import com.milesight.beaveriot.cache.BatchableCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author leon
 */
public class BatchableRedisCache extends RedisCache implements BatchableCache {

    private final RedisConnectionFactory redisConnectionFactory;

    protected BatchableRedisCache(RedisConnectionFactory redisConnectionFactory, String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfiguration) {
        super(name, cacheWriter, cacheConfiguration);
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public List<Object> multiGet(Object[] keys) {

        byte[][] binaryKeys = serializeCacheKeys(keys);

        try (RedisConnection connection = this.redisConnectionFactory.getConnection()) {
            List<byte[]> binaryValues = connection.stringCommands().mGet(binaryKeys);
            if (ObjectUtils.isEmpty(binaryValues)) {
                return null;
            }
            return binaryValues.stream().map(binaryValue -> binaryValue == null ? null : deserializeCacheValue(binaryValue)).toList();
        }
    }

    private byte[][] serializeCacheKeys(Object[] keys) {
        return Arrays.stream(keys).map(key->serializeCacheKey(createCacheKey(key))).toArray(byte[][]::new);
    }

    @Override
    public Long multiEvict(Object[] keys) {
        byte[][] binaryKeys = serializeCacheKeys(keys);
        try (RedisConnection connection = this.redisConnectionFactory.getConnection()) {
            return connection.keyCommands().del(binaryKeys);
        }
    }
}
