package com.milesight.beaveriot.base.executor;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author leon
 */
@Data
@Component
@ConfigurationProperties(TaskExecutorProperties.TASK_EXECUTOR_PROPERTY_PREFIX)
public class TaskExecutorProperties {

    public static final String TASK_EXECUTOR_PROPERTY_PREFIX = "task.executor";

    public static final String PRIMARY_EXECUTOR_ID = "default";

    public static final String DEFAULT_TASK_PREFIX = "task-";

    public static final Integer DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private int maxQueueCapacity = Integer.MAX_VALUE;

    private Duration gracefulShutdownTimeout;

    private Map<String, ExecutorPoolProperties> pool = new LinkedHashMap<>();

    @Data
    public static class ExecutorPoolProperties {

        private int queueCapacity = Integer.MAX_VALUE;

        private int coreSize = DEFAULT_THREAD_POOL_SIZE;

        private int maxSize = DEFAULT_THREAD_POOL_SIZE;

        private boolean allowCoreThreadTimeout = true;

        private Duration keepAlive = Duration.ofSeconds(60L);

        private boolean acceptTasksAfterContextClose;

        private boolean awaitTermination;

        private Duration awaitTerminationPeriod;

        private String threadNamePrefix;

        private boolean transmittable = true;

        private TaskExecutorFactoryBean.RejectedPolicy rejectedPolicy;

    }
}
