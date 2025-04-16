package com.milesight.beaveriot.cache.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.milesight.beaveriot.cache.autoconfigure.CustomizeCacheProperties;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author leon
 */
public class CustomizeCaffeineCacheManager extends CaffeineCacheManager {
    private final CustomizeCacheProperties.Specs specs;

    public CustomizeCaffeineCacheManager(CustomizeCacheProperties.Specs specs, String... cacheNames) {
        super(cacheNames);
        this.specs = specs;
    }

    public CustomizeCaffeineCacheManager(CustomizeCacheProperties.Specs specs) {
        this.specs = specs;
    }

    @Override
    protected Cache<Object, Object> createNativeCaffeineCache(String name) {
        Duration matchTimeToLive = specs.getMatchTimeToLive(name);
        if (matchTimeToLive != null) {
            return Caffeine.newBuilder()
                    .expireAfterAccess(matchTimeToLive.getSeconds(), TimeUnit.SECONDS)
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
                    .expireAfterAccess(matchTimeToLive.getSeconds(), TimeUnit.SECONDS)
                    .buildAsync();
        } else {
            return super.createAsyncCaffeineCache(name);
        }
    }
}
