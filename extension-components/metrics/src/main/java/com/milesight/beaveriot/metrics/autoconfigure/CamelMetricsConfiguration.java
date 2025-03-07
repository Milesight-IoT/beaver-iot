package com.milesight.beaveriot.metrics.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "camel.metrics")
public class CamelMetricsConfiguration {


    /**
     * Set whether to enable the MicrometerRoutePolicyFactory for capturing metrics
     * on route processing times.
     */
    private boolean enableRoutePolicy = true;


    /**
     * Set whether to enable the MicrometerRouteEventNotifier for capturing metrics
     * on the total number of routes and total number of routes running.
     */
    private boolean enableRouteEventNotifier = true;

    /**
     * Threshold configuration that triggers metric degradation events
     */
    private ThresholdConfig thresholdConfig = new ThresholdConfig();

    @Data
    public class ThresholdConfig {
        private int exchangeRepeatMax = 5;
    }
}
