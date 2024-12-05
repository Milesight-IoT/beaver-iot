package com.milesight.beaveriot.eventbus.configuration;

import com.lmax.disruptor.dsl.Disruptor;
import com.milesight.beaveriot.eventbus.AnnotationEventBusRegister;
import com.milesight.beaveriot.eventbus.DisruptorEventBus;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import com.milesight.beaveriot.eventbus.invoke.ListenerParameterResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
@ConditionalOnClass(Disruptor.class)
@Configuration
@EnableConfigurationProperties(DisruptorOptions.class)
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AnnotationEventBusRegister annotationEventBusRegister(DisruptorEventBus<? extends Event<? extends IdentityKey>> eventBus){
        return new AnnotationEventBusRegister(eventBus);
    }

    @Bean
    @ConditionalOnMissingBean
    public <T extends Event<? extends IdentityKey>> DisruptorEventBus<T> eventBus(DisruptorOptions disruptorOptions, ListenerParameterResolver listenerParameterResolver){
        return new DisruptorEventBus<>(disruptorOptions, listenerParameterResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public ListenerParameterResolver listenerParameterResolver(){
        return new ListenerParameterResolver();
    }

    @Bean
    public Executor eventBusTaskExecutor(DisruptorOptions disruptorOptions) {
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
