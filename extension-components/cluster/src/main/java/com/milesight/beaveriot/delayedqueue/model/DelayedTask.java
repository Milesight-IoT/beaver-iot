package com.milesight.beaveriot.delayedqueue.model;

import lombok.Data;
import org.springframework.util.Assert;

/**
 * author: Luxb
 * create: 2025/11/13 9:19
 **/
@Data
public class DelayedTask<T> {
    private String taskId;
    private T payload;

    private DelayedTask(String taskId, T payload) {
        Assert.notNull(taskId, "taskId cannot be null");

        this.taskId = taskId;
        this.payload = payload;
    }

    public static <T> DelayedTask<T> of(String taskId, T payload) {
        return new DelayedTask<>(taskId, payload);
    }
}