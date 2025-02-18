package com.milesight.beaveriot.eventbus.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author leon
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "eventbus.execution")
public class ExecutionOptions {

    private boolean enabled = true;

    private int corePoolSize = 16;

    private int maxPoolSize = 50;

    private int queueCapacity = 10000;

    private String eventBusTaskExecutor = "eventBusTaskExecutor";

    public static ExecutionOptions defaultOptions() {
        return new ExecutionOptions();
    }

}
