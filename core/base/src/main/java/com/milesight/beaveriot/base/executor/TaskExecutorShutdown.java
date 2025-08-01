package com.milesight.beaveriot.base.executor;

import lombok.extern.slf4j.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

/**
 * @author leon
 */
@Slf4j
public class TaskExecutorShutdown implements DisposableBean {

    @Autowired
    private ObjectProvider<TaskExecutor> taskExecutors;

    @Autowired
    private TaskExecutorProperties taskExecutorProperties;

    @Override
    public void destroy() {
        if (taskExecutors == null
                || taskExecutorProperties.getGracefulShutdownTimeout() == null
                || taskExecutorProperties.getGracefulShutdownTimeout().isZero()
                || taskExecutorProperties.getGracefulShutdownTimeout().isNegative()) {
            return;
        }

        // graceful shutdown
        taskExecutors.stream().forEach(taskExecutor -> {
            if (taskExecutor instanceof TtlTaskExecutors.TaskExecutorTtlWrapper wrapper) {
                doShutdown(wrapper.unwrap());
            } else if (taskExecutor instanceof ThreadPoolTaskExecutor threadPoolTaskExecutor) {
                doShutdown(threadPoolTaskExecutor);
            }
        });
    }

    private void doShutdown(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        if (!threadPoolTaskExecutor.getThreadPoolExecutor().isTerminated()) {
            threadPoolTaskExecutor.shutdown();
            boolean terminated = false;
            try {
                var timeout = taskExecutorProperties.getGracefulShutdownTimeout();
                log.info("ThreadPoolExecutor {} is shutting down. Timeout: {} seconds.", threadPoolTaskExecutor.getThreadNamePrefix(), timeout.getSeconds());
                terminated = threadPoolTaskExecutor.getThreadPoolExecutor()
                        .awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("ThreadPoolExecutor shutdown interrupted.", e);
                Thread.currentThread().interrupt();
            }
            if (!terminated) {
                threadPoolTaskExecutor.getThreadPoolExecutor().shutdownNow();
            }
        }
        log.debug("ThreadPoolExecutor {} terminated.", threadPoolTaskExecutor.getThreadNamePrefix());
    }
}
