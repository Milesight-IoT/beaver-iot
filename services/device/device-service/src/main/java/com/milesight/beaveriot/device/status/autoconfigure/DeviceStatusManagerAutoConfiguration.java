package com.milesight.beaveriot.device.status.autoconfigure;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import com.milesight.beaveriot.device.status.local.DeviceStatusLocalManager;
import com.milesight.beaveriot.device.status.redis.DeviceStatusRedisManager;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author: Luxb
 * create: 2025/9/4 8:58
 **/
@Configuration
public class DeviceStatusManagerAutoConfiguration {
    @Bean
    @ConditionalOnExpression("!'${spring.redis.host:}'.isEmpty()")
    public DeviceStatusManager deviceStatusRedisManager(DeviceServiceProvider deviceServiceProvider,
                                                        EntityServiceProvider entityServiceProvider,
                                                        EntityValueServiceProvider entityValueServiceProvider,
                                                        EntityTemplateServiceProvider entityTemplateServiceProvider,
                                                        RedissonClient redissonClient) {
        return new DeviceStatusRedisManager(deviceServiceProvider,
                entityServiceProvider,
                entityValueServiceProvider,
                entityTemplateServiceProvider,
                redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean({DeviceStatusManager.class})
    public DeviceStatusManager deviceStatusLocalManager(DeviceServiceProvider deviceServiceProvider,
                                                        EntityServiceProvider entityServiceProvider,
                                                        EntityValueServiceProvider entityValueServiceProvider,
                                                        EntityTemplateServiceProvider entityTemplateServiceProvider) {
        return new DeviceStatusLocalManager(deviceServiceProvider,
                entityServiceProvider,
                entityValueServiceProvider,
                entityTemplateServiceProvider);
    }
}
