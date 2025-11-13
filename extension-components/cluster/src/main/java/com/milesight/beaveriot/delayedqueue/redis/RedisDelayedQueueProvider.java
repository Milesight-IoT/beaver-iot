package com.milesight.beaveriot.delayedqueue.redis;

import com.milesight.beaveriot.delayedqueue.DelayedQueueProvider;
import com.milesight.beaveriot.delayedqueue.DelayedQueue;
import org.redisson.api.RedissonClient;

/**
 * author: Luxb
 * create: 2025/11/13 10:51
 **/
public class RedisDelayedQueueProvider implements DelayedQueueProvider {
    private final RedissonClient redissonClient;

    public RedisDelayedQueueProvider(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> DelayedQueue<T> create(String queueName) {
        return new RedisDelayedQueue<>(redissonClient, queueName);
    }
}
