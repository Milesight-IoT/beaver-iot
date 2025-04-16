package com.milesight.beaveriot.cache.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.cache.autoconfigure.CustomizeCacheProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnClass({Caffeine.class, CaffeineCacheManager.class})
@ConditionalOnMissingBean({CacheManager.class})
@AutoConfigureBefore({CacheAutoConfiguration.class})
@EnableConfigurationProperties({CustomizeCacheProperties.class, CacheProperties.class})
public class CustomizeCaffeineCacheAutoConfiguration {
    CustomizeCaffeineCacheAutoConfiguration() {
    }

    @Bean
    CaffeineCacheManager cacheManager(CacheProperties cacheProperties,  ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec, ObjectProvider<CacheLoader<Object, Object>> cacheLoader, CustomizeCacheProperties customizeCacheProperties) {
        CustomizeCacheProperties.Specs specs = customizeCacheProperties.getSpecs();
        CaffeineCacheManager cacheManager = this.createCacheManager(cacheProperties, caffeine, caffeineSpec, cacheLoader, specs);
        List<String> cacheNames = cacheProperties.getCacheNames();

        Set<String> ttlCaches = specs.getTimeToLives().keySet().stream()
                .map(k -> CustomizeCacheProperties.Specs.unwrapPrefixAndSubfix(k))
                .filter(k -> !k.contains(StringConstant.STAR))
                .collect(Collectors.toSet());

        if (!CollectionUtils.isEmpty(cacheNames) || !CollectionUtils.isEmpty(ttlCaches)) {
            ttlCaches.addAll(cacheNames);
            cacheManager.setCacheNames(ttlCaches);
        }

        return cacheManager;
    }

    private CaffeineCacheManager createCacheManager(CacheProperties cacheProperties, ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec, ObjectProvider<CacheLoader<Object, Object>> cacheLoader, CustomizeCacheProperties.Specs specs) {
        CaffeineCacheManager cacheManager = new CustomizeCaffeineCacheManager(specs);
        this.setCacheBuilder(cacheProperties, caffeineSpec.getIfAvailable(), caffeine.getIfAvailable(), cacheManager);
        Objects.requireNonNull(cacheManager);
        cacheLoader.ifAvailable(cacheManager::setCacheLoader);
        return cacheManager;
    }

    private void setCacheBuilder(CacheProperties cacheProperties, CaffeineSpec caffeineSpec, Caffeine<Object, Object> caffeine, CaffeineCacheManager cacheManager) {
        String specification = cacheProperties.getCaffeine().getSpec();
        if (StringUtils.hasText(specification)) {
            cacheManager.setCacheSpecification(specification);
        } else if (caffeineSpec != null) {
            cacheManager.setCaffeineSpec(caffeineSpec);
        } else if (caffeine != null) {
            cacheManager.setCaffeine(caffeine);
        }

    }
}
