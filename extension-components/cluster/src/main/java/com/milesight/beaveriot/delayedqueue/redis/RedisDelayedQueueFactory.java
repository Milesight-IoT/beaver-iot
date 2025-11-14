package com.milesight.beaveriot.delayedqueue.redis;

import com.milesight.beaveriot.delayedqueue.DelayedQueueFactory;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import org.redisson.api.RedissonClient;

/**
 * author: Luxb
 * create: 2025/11/13 10:51
 **/
public class RedisDelayedQueueFactory implements DelayedQueueFactory {
    private final RedissonClient redissonClient;

    public RedisDelayedQueueFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> DelayedQueue<T> create(String queueName) {
        return new RedisDelayedQueue<>(redissonClient, queueName);
    }
}
