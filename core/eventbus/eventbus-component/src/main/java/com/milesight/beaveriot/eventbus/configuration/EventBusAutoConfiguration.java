package com.milesight.beaveriot.eventbus.configuration;

import com.milesight.beaveriot.eventbus.AnnotationEventBusRegister;
import com.milesight.beaveriot.eventbus.EventBusDispatcher;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import com.milesight.beaveriot.eventbus.invoke.ListenerParameterResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leon
 */
@Configuration
@EnableConfigurationProperties(ExecutionOptions.class)
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AnnotationEventBusRegister annotationEventBusRegister(EventBusDispatcher<? extends Event<? extends IdentityKey>> eventBus){
        return new AnnotationEventBusRegister(eventBus);
    }

    @Bean
    @ConditionalOnMissingBean
    public <T extends Event<? extends IdentityKey>> EventBusDispatcher<T> eventBus(ExecutionOptions executionOptions, ListenerParameterResolver listenerParameterResolver){
        return new EventBusDispatcher<>(executionOptions, listenerParameterResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public ListenerParameterResolver listenerParameterResolver(){
        return new ListenerParameterResolver();
    }

    @Bean
    public Executor eventBusTaskExecutor(ExecutionOptions disruptorOptions) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(disruptorOptions.getCorePoolSize());
        executor.setMaxPoolSize(disruptorOptions.getMaxPoolSize());
        executor.setQueueCapacity(disruptorOptions.getQueueCapacity());
        executor.setThreadNamePrefix("eventBus-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
