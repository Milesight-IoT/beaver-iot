package com.milesight.beaveriot.permission.initializer;

import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.spi.TtlEnhanced;
import com.alibaba.ttl.spi.TtlWrapper;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.alibaba.ttl.threadpool.agent.TtlAgent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leon
 */
public class TaskExecutionApplicationContextInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        applicationContext.getBeanFactory().addBeanPostProcessor(new SmartInstantiationAwareBeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

                if (bean instanceof ThreadPoolTaskExecutor taskExecutor) {
                    return needWrapTaskExecutor(taskExecutor) ? new TaskExecutorTtlWrapper(taskExecutor) : taskExecutor;
                } else if (bean instanceof ThreadPoolExecutor executor)  {
                    return needWrapTaskExecutor(executor) ? TtlExecutors.getTtlExecutor(executor) : executor;
                } else {
                    return bean;
                }
            }

            private boolean needWrapTaskExecutor(Executor taskExecutor) {
                return !(TtlAgent.isTtlAgentLoaded() || null == taskExecutor || taskExecutor instanceof TtlEnhanced);
            }
        });
    }

    public class TaskExecutorTtlWrapper implements TaskExecutor, TtlWrapper<Executor>, TtlEnhanced, ApplicationListener<ContextClosedEvent> {
        private final ThreadPoolTaskExecutor executor;

        TaskExecutorTtlWrapper(ThreadPoolTaskExecutor executor) {
            this.executor = executor;
        }

        @Override
        public void execute( Runnable task) {
            executor.execute(TtlRunnable.get(task));
        }

        public TaskExecutor getExecutor() {
            return executor;
        }

        @Override
        public Executor unwrap() {
            return executor;
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
        public void onApplicationEvent(ContextClosedEvent event) {
            executor.onApplicationEvent(event);
        }
    }
}
