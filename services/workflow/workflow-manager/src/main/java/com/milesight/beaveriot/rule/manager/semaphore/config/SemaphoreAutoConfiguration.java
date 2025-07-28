package com.milesight.beaveriot.rule.manager.semaphore.config;

import com.milesight.beaveriot.rule.manager.semaphore.DistributedSemaphore;
import com.milesight.beaveriot.rule.manager.semaphore.local.LocalSemaphore;
import com.milesight.beaveriot.rule.manager.semaphore.redis.RedisSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author: Luxb
 * create: 2025/7/25 15:17
 **/
@Configuration
public class SemaphoreAutoConfiguration {
    @Bean
    @ConditionalOnExpression("!'${spring.redis.host:}'.isEmpty()")
    public DistributedSemaphore redisSemaphore(RedissonClient redissonClient) {
        return new RedisSemaphore(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean({DistributedSemaphore.class})
    public DistributedSemaphore localSemaphore() {
        return new LocalSemaphore();
    }
}
