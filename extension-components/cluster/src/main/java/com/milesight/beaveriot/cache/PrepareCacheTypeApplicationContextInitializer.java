package com.milesight.beaveriot.cache;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
public class PrepareCacheTypeApplicationContextInitializer implements ApplicationContextInitializer {

    private static final String CACHE_TYPE_PROPERTY = "spring.cache.type";
    private static final String SPRING_REDIS_HOST = "spring.data.redis.host";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (ObjectUtils.isEmpty(applicationContext.getEnvironment().getProperty(CACHE_TYPE_PROPERTY)) &&
                ObjectUtils.isEmpty(applicationContext.getEnvironment().getProperty(SPRING_REDIS_HOST)) ) {
            applicationContext.getEnvironment().getSystemProperties().put(CACHE_TYPE_PROPERTY, "caffeine");
        }
    }
}
