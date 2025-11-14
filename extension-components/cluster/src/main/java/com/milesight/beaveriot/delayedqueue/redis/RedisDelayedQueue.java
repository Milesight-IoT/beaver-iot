package com.milesight.beaveriot.delayedqueue.redis;

import com.milesight.beaveriot.delayedqueue.BaseDelayedQueue;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

/**
 * author: Luxb
 * create: 2025/11/13 9:25
 **/
@Slf4j
public class RedisDelayedQueue<T> extends BaseDelayedQueue<T> {
    public RedisDelayedQueue(RedissonClient redissonClient, String queueName) {
        super(queueName);
        this.delayQueue = new RedisDelayedQueueWrapper<>(redissonClient, queueName);
        this.taskExpireTimeMap = redissonClient.getMap(queueName + Constants.SUFFIX_EXPIRE_TIME);
    }

    private static class Constants {
        private static final String SUFFIX_EXPIRE_TIME = ":expire-time";
    }
}