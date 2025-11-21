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
        super(queueName, new RedisDelayedQueueWrapper<>(redissonClient, queueName), redissonClient.getMap(getExpireTimeMapName(queueName)));
    }

    private static String getExpireTimeMapName(String queueName) {
        return String.format(Constants.EXPIRE_TIME_MAP_NAME_FORMAT, queueName);
    }

    private static class Constants {
        private static final String EXPIRE_TIME_MAP_NAME_FORMAT = "delayed-queue-expire-time:{%s}";
    }
}