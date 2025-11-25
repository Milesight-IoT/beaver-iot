package com.milesight.beaveriot.redisson.autoconfigure;

import org.redisson.Redisson;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisOperations;

/**
 * @author leon
 */
@AutoConfiguration(before = {RedisAutoConfiguration.class})
@ConditionalOnClass({Redisson.class, RedisOperations.class})
@EnableConfigurationProperties({RedissonProperties.class, RedisProperties.class})
@ConditionalOnExpression("!'${spring.data.redis.host:}'.isEmpty()")
public class CustomizeRedissonAutoConfiguration extends RedissonAutoConfiguration {
    public CustomizeRedissonAutoConfiguration() {
    }
}
