package com.milesight.beaveriot.cache.redis;

import com.milesight.beaveriot.cache.autoconfigure.CustomizeCacheProperties;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CustomizeRedisCacheManager extends RedisCacheManager {
    private final CustomizeCacheProperties.Specs redisConfig;

    private final RedisConnectionFactory redisConnectionFactory;

    public CustomizeRedisCacheManager(RedisConnectionFactory redisConnectionFactory, RedisCacheWriter redisCacheManager, RedisCacheConfiguration redisCacheConfiguration, Map<String, RedisCacheConfiguration> initialCacheConfiguration, boolean allowInFlightCacheCreation , CustomizeCacheProperties.Specs redisConfig) {
        super(redisCacheManager, redisCacheConfiguration,initialCacheConfiguration,allowInFlightCacheCreation);
        this.redisConfig = redisConfig;
        this.redisConnectionFactory = redisConnectionFactory;
    }
    
    @Override
    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        if(redisConfig != null){
            Duration ttl = redisConfig.getMatchTimeToLive(name);
            if(ttl != null){
                cacheConfig = cacheConfig.entryTtl(ttl);
            }
        }
        return new BatchableRedisCache(redisConnectionFactory, name, getCacheWriter(), resolveCacheConfiguration(cacheConfig));
    }

    private RedisCacheConfiguration resolveCacheConfiguration(@Nullable RedisCacheConfiguration cacheConfiguration) {
        return cacheConfiguration != null ? cacheConfiguration : getDefaultCacheConfiguration();
    }

    public static CustomizeRedisCacheManagerBuilder customizeBuilder(RedisConnectionFactory redisConnectionFactory, CustomizeCacheProperties redisConfig) {
        return CustomizeRedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory, redisConfig);
    }

    public static class CustomizeRedisCacheManagerBuilder {

        private CustomizeCacheProperties redisConfig;
        private RedisConnectionFactory connectionFactory;
        public static CustomizeRedisCacheManagerBuilder fromConnectionFactory(RedisConnectionFactory connectionFactory, CustomizeCacheProperties redisConfig) {

            Assert.notNull(connectionFactory, "ConnectionFactory must not be null");

            RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);

            return new CustomizeRedisCacheManagerBuilder(connectionFactory, cacheWriter, redisConfig);
        }

        private boolean allowRuntimeCacheCreation = true;
        private boolean enableTransactions = true;

        private CacheStatisticsCollector statisticsCollector = CacheStatisticsCollector.none();

        private final Map<String, RedisCacheConfiguration> initialCaches = new LinkedHashMap<>();

        private RedisCacheConfiguration defaultCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();

        private @Nullable RedisCacheWriter cacheWriter;

        private CustomizeRedisCacheManagerBuilder(RedisConnectionFactory connectionFactory, RedisCacheWriter cacheWriter, CustomizeCacheProperties redisConfig) {
            this.cacheWriter = cacheWriter;
            this.redisConfig = redisConfig;
            this.connectionFactory = connectionFactory;
        }

        public CustomizeRedisCacheManagerBuilder allowCreateOnMissingCache(boolean allowRuntimeCacheCreation) {
            this.allowRuntimeCacheCreation = allowRuntimeCacheCreation;
            return this;
        }

        public RedisCacheConfiguration cacheDefaults() {
            return this.defaultCacheConfiguration;
        }

        public CustomizeRedisCacheManagerBuilder cacheDefaults(RedisCacheConfiguration defaultCacheConfiguration) {

            Assert.notNull(defaultCacheConfiguration, "DefaultCacheConfiguration must not be null");

            this.defaultCacheConfiguration = defaultCacheConfiguration;

            return this;
        }

        public CustomizeRedisCacheManagerBuilder cacheWriter(RedisCacheWriter cacheWriter) {

            Assert.notNull(cacheWriter, "CacheWriter must not be null");

            this.cacheWriter = cacheWriter;
            return this;
        }

        public CustomizeRedisCacheManagerBuilder enableStatistics() {
            this.statisticsCollector = CacheStatisticsCollector.create();
            return this;
        }

        public CustomizeRedisCacheManagerBuilder initialCacheNames(Set<String> cacheNames) {

            Assert.notNull(cacheNames, "CacheNames must not be null");
            Assert.noNullElements(cacheNames, "CacheNames must not be null");

            cacheNames.forEach(it -> withCacheConfiguration(it, defaultCacheConfiguration));

            return this;
        }
        public CustomizeRedisCacheManagerBuilder transactionAware(boolean enabled) {
            this.enableTransactions = enabled;
            return this;
        }

        public CustomizeRedisCacheManagerBuilder withCacheConfiguration(String cacheName,
                                                                                 RedisCacheConfiguration cacheConfiguration) {

            Assert.notNull(cacheName, "CacheName must not be null");
            Assert.notNull(cacheConfiguration, "CacheConfiguration must not be null");

            this.initialCaches.put(cacheName, cacheConfiguration);

            return this;
        }

        public CustomizeRedisCacheManager build() {

            Assert.state(cacheWriter != null, "CacheWriter must not be null;"
                    + " You can provide one via 'RedisCacheManagerBuilder#cacheWriter(RedisCacheWriter)'");

            RedisCacheWriter resolvedCacheWriter = !CacheStatisticsCollector.none().equals(this.statisticsCollector)
                    ? this.cacheWriter.withStatisticsCollector(this.statisticsCollector)
                    : this.cacheWriter;

            CustomizeRedisCacheManager cacheManager = newRedisCacheManager(resolvedCacheWriter);

            cacheManager.setTransactionAware(this.enableTransactions);

            return cacheManager;
        }

        private CustomizeRedisCacheManager newRedisCacheManager(RedisCacheWriter cacheWriter) {
            return new CustomizeRedisCacheManager(connectionFactory, cacheWriter, cacheDefaults(), this.initialCaches,this.allowRuntimeCacheCreation, this.redisConfig.getSpecs());
        }
    }
}