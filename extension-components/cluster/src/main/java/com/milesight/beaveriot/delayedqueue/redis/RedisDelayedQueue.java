package com.milesight.beaveriot.delayedqueue.redis;

import com.milesight.beaveriot.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.delayedqueue.model.DelayedTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/11/13 9:25
 **/
@Slf4j
public class RedisDelayedQueue<T> implements DelayedQueue<T> {
    private final RedissonClient redissonClient;
    private final String queueName;

    public RedisDelayedQueue(RedissonClient redissonClient, String queueName) {
        this.redissonClient = redissonClient;
        this.queueName = queueName;
    }

    @Override
    public void offer(DelayedTask<T> task, Duration delay) {
        log.info("offer from redis: taskId:{}", task.getTaskId());
    }

    @Override
    public void cancel(String taskId) {

    }

    @Override
    public DelayedTask<T> take() {
        return null;
    }

    @Override
    public DelayedTask<T> poll() {
        return null;
    }
}