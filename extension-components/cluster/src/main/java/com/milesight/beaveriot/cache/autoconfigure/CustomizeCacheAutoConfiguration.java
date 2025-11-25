package com.milesight.beaveriot.cache.autoconfigure;

import com.milesight.beaveriot.cache.redis.CustomizeRedisCacheManager;
import lombok.SneakyThrows;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.BatchCacheAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author leon
 */
@ConditionalOnClass(CacheManager.class)
@AutoConfiguration(after = {  RedisAutoConfiguration.class })
@AutoConfigureBefore({CacheAutoConfiguration.class})
@EnableConfigurationProperties({CustomizeCacheProperties.class, CacheProperties.class})
public class CustomizeCacheAutoConfiguration {
    @Bean
    @ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty()")
    CustomizeRedisCacheManager cacheManger(CacheProperties cacheProperties,CustomizeCacheProperties customizeCacheProperties,
                                  ObjectProvider<org.springframework.data.redis.cache.RedisCacheConfiguration> redisCacheConfiguration,
                                  RedisConnectionFactory redisConnectionFactory, ResourceLoader resourceLoader) {
        CustomizeRedisCacheManager.CustomizeRedisCacheManagerBuilder builder = CustomizeRedisCacheManager.customizeBuilder(redisConnectionFactory, customizeCacheProperties)
                .transactionAware(true)
                .cacheDefaults(
                        determineConfiguration(cacheProperties, customizeCacheProperties, redisCacheConfiguration, resourceLoader.getClassLoader()));
        List<String> cacheNames = cacheProperties.getCacheNames();
        if (!cacheNames.isEmpty()) {
            builder.initialCacheNames(new LinkedHashSet<>(cacheNames));
        }
        if (cacheProperties.getRedis().isEnableStatistics()) {
            builder.enableStatistics();
        }
        return builder.build();
    }

    private org.springframework.data.redis.cache.RedisCacheConfiguration determineConfiguration(
            CacheProperties cacheProperties, CustomizeCacheProperties customizeCacheProperties,
            ObjectProvider<org.springframework.data.redis.cache.RedisCacheConfiguration> redisCacheConfiguration,
            ClassLoader classLoader) {
        return redisCacheConfiguration.getIfAvailable(() -> createConfiguration(cacheProperties, classLoader,customizeCacheProperties));
    }

    private org.springframework.data.redis.cache.RedisCacheConfiguration createConfiguration(
            CacheProperties cacheProperties, ClassLoader classLoader, CustomizeCacheProperties customizeCacheProperties) {
        CustomizeCacheProperties.RedisConfig redisConfig = customizeCacheProperties.getRedis();
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration
                .defaultCacheConfig();

        if (!ObjectUtils.isEmpty(redisConfig.getValueSerializerClass())) {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(newInstance(redisConfig.getValueSerializerClass())));
        }else {
            config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(classLoader)));
        }
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }

    @SneakyThrows
    private RedisSerializer newInstance(String clazz) {
        Object o = ClassUtils.forName(clazz, CustomizeCacheAutoConfiguration.class.getClassLoader()).newInstance();
        if(o instanceof RedisSerializer){
            return (RedisSerializer) o;
        }else{
            throw new UnsupportedOperationException("class "+clazz+" is not RedisSerializer");
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public BatchCacheAspect batchCacheAspect() {
        return new BatchCacheAspect();
    }
}
