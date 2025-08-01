package com.milesight.beaveriot.base.executor;

import lombok.*;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leon
 */
@Data
public class TaskExecutorFactoryBean implements FactoryBean<TaskExecutor> {

    private Integer queueCapacity;

    private Integer corePoolSize;

    private Integer maxPoolSize;

    private Boolean allowCoreThreadTimeOut;

    private Duration keepAlive;

    private Boolean awaitTermination;

    private Duration awaitTerminationPeriod;

    private String threadNamePrefix;

    private boolean transmittable;

    private RejectedPolicy rejectedPolicy;

    @Override
    public TaskExecutor getObject() {
        var builder = new ThreadPoolTaskExecutorBuilder();
        builder = builder.queueCapacity(queueCapacity);
        builder = builder.corePoolSize(corePoolSize);
        builder = builder.maxPoolSize(maxPoolSize);
        builder = builder.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        builder = builder.keepAlive(keepAlive);

        if (awaitTermination != null) {
            builder = builder.awaitTermination(awaitTermination);
        }

        if (awaitTerminationPeriod != null) {
            builder = builder.awaitTerminationPeriod(awaitTerminationPeriod);
        }

        builder = builder.threadNamePrefix(threadNamePrefix);
        if (rejectedPolicy != null) {
            builder = builder.additionalCustomizers(customizer -> customizer.setRejectedExecutionHandler(rejectedPolicy.newInstance()));
        }

        var taskExecutor = builder.build();
        taskExecutor.initialize();
        if (transmittable) {
            return TtlTaskExecutors.getExecutor(taskExecutor);
        }
        return taskExecutor;
    }

    @Override
    public Class<?> getObjectType() {
        return ThreadPoolTaskExecutor.class;
    }

    public enum RejectedPolicy {
        ABORT_POLICY(ThreadPoolExecutor.AbortPolicy.class),
        CALLER_RUNS_POLICY(ThreadPoolExecutor.CallerRunsPolicy.class),
        DISCARD_POLICY(ThreadPoolExecutor.DiscardPolicy.class),
        DISCARD_OLDEST_POLICY(ThreadPoolExecutor.DiscardOldestPolicy.class),
        BLOCK_POLICY(BlockPolicy.class);

        private final Class<? extends RejectedExecutionHandler> clazz;

        RejectedPolicy(Class<? extends RejectedExecutionHandler> clazz) {
            this.clazz = clazz;
        }

        public Class<? extends RejectedExecutionHandler> getClazz() {
            return clazz;
        }

        @SneakyThrows
        public RejectedExecutionHandler newInstance() {
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
}
