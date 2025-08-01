package com.milesight.beaveriot.base.executor;

import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.spi.TtlEnhanced;
import com.alibaba.ttl.spi.TtlWrapper;
import com.alibaba.ttl.threadpool.agent.TtlAgent;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;


/**
 * @author leon
 */
@Slf4j
public class TtlTaskExecutors {

    private TtlTaskExecutors() {
        throw new UnsupportedOperationException();
    }

    public static TaskExecutor getExecutor(ThreadPoolTaskExecutor executor) {
        if (TtlAgent.isTtlAgentLoaded() || null == executor || executor instanceof TtlEnhanced) {
            return executor;
        }
        return new TaskExecutorTtlWrapper(executor);
    }

    public static class TaskExecutorTtlWrapper implements TaskExecutor, TtlWrapper<Executor>, TtlEnhanced, ApplicationListener<ContextClosedEvent> {
        private final ThreadPoolTaskExecutor executor;

        TaskExecutorTtlWrapper(ThreadPoolTaskExecutor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (log.isTraceEnabled()) {
                printMetric();
            }
            executor.execute(TtlRunnable.get(command));
        }

        public TaskExecutor getExecutor() {
            return executor;
        }

        @Override
        public @NonNull ThreadPoolTaskExecutor unwrap() {
            return executor;
        }

        public void printMetric() {
            int activeCount = executor.getActiveCount();
            int corePoolSize = executor.getCorePoolSize();
            int maxPoolSize = executor.getMaxPoolSize();
            int queueTaskSize = executor.getThreadPoolExecutor().getQueue() != null ? executor.getThreadPoolExecutor().getQueue().size() : -1;
            long completedTaskCount = executor.getThreadPoolExecutor().getCompletedTaskCount();

            log.trace("name='{}',activeCount='{}',queueSize='{}',corePoolSize='{}',maxPoolSize='{}',completedTaskCount='{}'",
                    executor.getThreadNamePrefix(), activeCount, queueTaskSize, corePoolSize, maxPoolSize, completedTaskCount);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TaskExecutorTtlWrapper that = (TaskExecutorTtlWrapper) o;

            return executor.equals(that.executor);
        }

        @Override
        public int hashCode() {
            return executor.hashCode();
        }

        @Override
        public String toString() {
            return this.getClass().getName() + " - " + executor.toString();
        }

        @Override
        public void onApplicationEvent(@NonNull ContextClosedEvent event) {
            executor.onApplicationEvent(event);
        }
    }
}
