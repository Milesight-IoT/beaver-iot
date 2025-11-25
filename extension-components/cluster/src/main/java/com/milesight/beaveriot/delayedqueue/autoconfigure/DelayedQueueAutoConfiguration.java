package com.milesight.beaveriot.delayedqueue.autoconfigure;

import com.milesight.beaveriot.delayedqueue.DelayedQueueFactory;
import com.milesight.beaveriot.delayedqueue.local.LocalDelayedQueueFactory;
import com.milesight.beaveriot.delayedqueue.redis.RedisDelayedQueueFactory;
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
    @ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty()")
    public DelayedQueueFactory redisDelayedQueueFactory(RedissonClient redissonClient) {
        return new RedisDelayedQueueFactory(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(DelayedQueueFactory.class)
    public DelayedQueueFactory localDelayedQueueFactory() {
        return new LocalDelayedQueueFactory();
    }
}