package com.milesight.beaveriot.cache.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.milesight.beaveriot.cache.autoconfigure.CustomizeCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author leon
 */
@Slf4j
public class CustomizeCaffeineCacheManager extends CaffeineCacheManager {
    private final CustomizeCacheProperties.Specs specs;

    private Map<String, org.springframework.cache.Cache> cacheMap;

    public CustomizeCaffeineCacheManager(CustomizeCacheProperties.Specs specs, String... cacheNames) {
        super(cacheNames);
        this.specs = specs;
        initCacheMap();
    }

    public CustomizeCaffeineCacheManager(CustomizeCacheProperties.Specs specs) {
        this.specs = specs;
        initCacheMap();
    }

    private void initCacheMap() {
        try {
            Field cacheMapField = CaffeineCacheManager.class.getDeclaredField("cacheMap");
            cacheMapField.setAccessible(true);
            //noinspection unchecked
            cacheMap = (Map<String, org.springframework.cache.Cache>) cacheMapField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // impossible
            log.error("init caffeine cache map failed", e);
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    protected Cache<Object, Object> createNativeCaffeineCache(String name) {
        Duration matchTimeToLive = specs.getMatchTimeToLive(name);
        if (matchTimeToLive != null) {
            return Caffeine.newBuilder()
                    .expireAfterWrite(matchTimeToLive.getSeconds(), TimeUnit.SECONDS)
                    .build();
        } else {
            return super.createNativeCaffeineCache(name);
        }
    }

    @Override
    protected AsyncCache<Object, Object> createAsyncCaffeineCache(String name) {
        Duration matchTimeToLive = specs.getMatchTimeToLive(name);
        if (matchTimeToLive != null) {
            return Caffeine.newBuilder()
                    .expireAfterWrite(matchTimeToLive.getSeconds(), TimeUnit.SECONDS)
                    .buildAsync();
        } else {
            return super.createAsyncCaffeineCache(name);
        }
    }

    @Override
    @Nullable
    public org.springframework.cache.Cache getCache(String name) {
        org.springframework.cache.Cache cache = super.getCache(name);
        if (cache == null) {
            cache = this.cacheMap.computeIfAbsent(name, this::createCaffeineCache);
        }
        return cache;
    }

    @Override
    protected org.springframework.cache.Cache createCaffeineCache(String name) {
        return new TransactionAwareCacheDecorator(super.createCaffeineCache(name));
    }

}
