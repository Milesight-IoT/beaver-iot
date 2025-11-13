package com.milesight.beaveriot.delayedqueue.redis;

import com.milesight.beaveriot.delayedqueue.BaseDelayedQueue;
import com.milesight.beaveriot.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.delayedqueue.model.DelayedTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

/**
 * author: Luxb
 * create: 2025/11/13 9:25
 **/
@Slf4j
public class RedisDelayedQueue<T> extends BaseDelayedQueue implements DelayedQueue<T> {
    private final RedissonClient redissonClient;

    public RedisDelayedQueue(RedissonClient redissonClient, String queueName) {
        super(queueName);
        this.redissonClient = redissonClient;
    }

    @Override
    public void offer(DelayedTask<T> task) {
        log.info("offer from redis: taskId:{}", task.getId());
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