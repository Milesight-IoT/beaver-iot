package com.milesight.beaveriot.mqtt.broker.bridge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Configuration
public class MqttBrokerBridgeConfiguration {

    @Value("${mqtt.broker.listener.parallelism:}")
    private Integer listenerParallelism;

    @Bean(name = "mqttBrokerBridgeListenerForkJoinPool", destroyMethod = "shutdown")
    public ForkJoinPool mqttBrokerBridgeListenerForkJoinPool() {
        int parallelism = listenerParallelism == null
                ? Runtime.getRuntime().availableProcessors() * 2
                : listenerParallelism;
        return new ForkJoinPool(parallelism, new ForkJoinPool.ForkJoinWorkerThreadFactory() {
            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                return new CustomClassLoaderForkJoinWorkerThread(pool);
            }

            static class CustomClassLoaderForkJoinWorkerThread extends ForkJoinWorkerThread {
                private CustomClassLoaderForkJoinWorkerThread(final ForkJoinPool pool) {
                    super(pool);
                    setContextClassLoader(Thread.currentThread().getContextClassLoader());
                }
            }
        } , null, true);
    }

}
