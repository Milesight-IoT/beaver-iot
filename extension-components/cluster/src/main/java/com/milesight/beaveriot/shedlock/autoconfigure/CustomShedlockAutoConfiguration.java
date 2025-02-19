package com.milesight.beaveriot.shedlock.autoconfigure;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.inmemory.InMemoryLockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author leon
 */
@ConditionalOnClass({SchedulerLock.class})
public class CustomShedlockAutoConfiguration {

    @Value("${shedlock.env:beaveriot}")
    private String env;

    @Bean
    @Primary
    @ConditionalOnBean({RedisConnectionFactory.class})
    public LockProvider redisLockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, env);
    }

    @Bean
    @ConditionalOnMissingBean({RedisConnectionFactory.class})
    public LockProvider memoryLockProvider() {
        return new InMemoryLockProvider();
    }

}
