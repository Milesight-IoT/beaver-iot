package com.milesight.beaveriot.delayedqueue.redis;

import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import com.milesight.beaveriot.delayedqueue.DelayedQueueWrapper;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * author: Luxb
 * create: 2025/11/14 10:46
 **/
public class RedisDelayedQueueWrapper<T> implements DelayedQueueWrapper<T> {
    private final RBlockingQueue<DelayedTask<T>> innerBlockingQueue;
    private final RDelayedQueue<DelayedTask<T>> innerDelayedQueue;

    public RedisDelayedQueueWrapper(RedissonClient redissonClient, String queueName) {
        this.innerBlockingQueue = redissonClient.getBlockingQueue(queueName);
        this.innerDelayedQueue = redissonClient.getDelayedQueue(innerBlockingQueue);
    }

    @Override
    public void offer(DelayedTask<T> task) {
        innerDelayedQueue.offer(task, task.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
    }

    @Override
    public DelayedTask<T> take() throws InterruptedException {
        return innerBlockingQueue.take();
    }

    @Override
    public boolean isEmpty() {
        return innerDelayedQueue.isEmpty() && innerBlockingQueue.isEmpty();
    }
}