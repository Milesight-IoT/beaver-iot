package com.milesight.beaveriot.coalescer.autoconfigure;

import com.milesight.beaveriot.coalescer.InMemoryRequestCoalescer;
import com.milesight.beaveriot.coalescer.RequestCoalescer;
import com.milesight.beaveriot.coalescer.redis.RedisRequestCoalescer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;

/**
 * Auto-configuration for Request Coalescer.
 *
 * @author simon
 */
@Slf4j
@AutoConfiguration(after = {RedisAutoConfiguration.class})
public class RequestCoalescerAutoConfiguration {
    @Bean
    @ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty()")
    @ConditionalOnMissingBean(RequestCoalescer.class)
    public <V> RequestCoalescer<V> redisRequestCoalescer(
            RedissonClient redissonClient, @Qualifier("request-coalescer") TaskExecutor executor) {
        log.info("Creating RedisRequestCoalescer with distributed coordination");
        return new RedisRequestCoalescer<>(redissonClient, executor);
    }

    @Bean
    @ConditionalOnMissingBean(RequestCoalescer.class)
    public <V> RequestCoalescer<V> inMemoryRequestCoalescer(@Qualifier("request-coalescer") TaskExecutor executor) {
        log.info("Creating InMemoryRequestCoalescer");
        return new InMemoryRequestCoalescer<>(executor);
    }
}
