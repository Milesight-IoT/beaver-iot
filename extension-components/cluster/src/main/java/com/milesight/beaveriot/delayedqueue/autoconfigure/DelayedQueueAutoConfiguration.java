package com.milesight.beaveriot.delayedqueue.autoconfigure;

import com.milesight.beaveriot.delayedqueue.DelayedQueueProvider;
import com.milesight.beaveriot.delayedqueue.local.LocalDelayedQueueProvider;
import com.milesight.beaveriot.delayedqueue.redis.RedisDelayedQueueProvider;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author: Luxb
 * create: 2025/11/13 9:24
 **/
@Configuration
public class DelayedQueueAutoConfiguration {
    @Bean
    @ConditionalOnExpression("!'${spring.redis.host:}'.isEmpty()")
    public DelayedQueueProvider redisDelayedQueueProvider(RedissonClient redissonClient) {
        return new RedisDelayedQueueProvider(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(DelayedQueueProvider.class)
    public DelayedQueueProvider localDelayedQueueProvider() {
        return new LocalDelayedQueueProvider();
    }
}