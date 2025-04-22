package com.milesight.beaveriot.shedlock.autoconfigure;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.RetryableLockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.inmemory.InMemoryLockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Optional;

/**
 * @author leon
 */
@ConditionalOnClass({SchedulerLock.class})
@Import(AutoProxyRegistrar.class)
public class CustomizeShedlockAutoConfiguration {

    @Value("${shedlock.env:beaveriot}")
    private String env;

    @Bean
    @Primary
    @ConditionalOnBean({RedisConnectionFactory.class})
    public LockProvider redisLockProvider(RedisConnectionFactory connectionFactory) {
        return new RetryableLockProvider(new ScopeRedisLockProvider(connectionFactory, env));
    }

    @Bean
    @ConditionalOnMissingBean({RedisConnectionFactory.class})
    public LockProvider memoryLockProvider() {
        return new RetryableLockProvider(new ScopeInMemoryLockProvider());
    }

    public class ScopeRedisLockProvider extends RedisLockProvider {

        public ScopeRedisLockProvider(RedisConnectionFactory connectionFactory, String env) {
            super(connectionFactory, env);
        }

        @Override
        public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
            return super.lock(lockConfiguration);
        }
    }

    public class ScopeInMemoryLockProvider extends InMemoryLockProvider {
        @Override
        public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
            return super.lock(lockConfiguration);
        }
    }

}
