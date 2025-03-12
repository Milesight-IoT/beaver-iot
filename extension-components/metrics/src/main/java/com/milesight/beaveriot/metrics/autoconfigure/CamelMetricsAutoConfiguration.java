package com.milesight.beaveriot.metrics.autoconfigure;

import com.milesight.beaveriot.metrics.camel.CamelMicrometerRoutePolicy;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.spi.RoutePolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

/**
 * @author leon
 */
@EnableConfigurationProperties({CamelMetricsConfiguration.class})
@Configuration
public class CamelMetricsAutoConfiguration {

    public CamelMetricsAutoConfiguration(CamelContext camelContext, CamelMetricsConfiguration configuration, MeterRegistry meterRegistry, ApplicationEventPublisher applicationEventPublisher) {
        if (meterRegistry != null) {
            configureMicrometer(camelContext, configuration, meterRegistry, applicationEventPublisher);
        }
    }

    private void configureMicrometer(CamelContext camelContext, CamelMetricsConfiguration configuration, MeterRegistry meterRegistry,ApplicationEventPublisher applicationEventPublisher) {

        if (configuration.isEnableRoutePolicy()) {
            MicrometerRoutePolicyFactory factory = new MicrometerRoutePolicyFactory() {
                @Override
                public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode routeDefinition) {
                    InflightRepository inflightRepository = camelContext.getInflightRepository();
                    CamelMicrometerRoutePolicy answer = new CamelMicrometerRoutePolicy(inflightRepository, applicationEventPublisher, configuration);
                    answer.setMeterRegistry(getMeterRegistry());
                    answer.setPrettyPrint(isPrettyPrint());
                    answer.setDurationUnit(getDurationUnit());
                    answer.setNamingStrategy(getNamingStrategy());
                    answer.setConfiguration(getPolicyConfiguration());
                    answer.start();
                    return answer;
                }
            };
            factory.setMeterRegistry(meterRegistry);
            camelContext.addRoutePolicyFactory(factory);
        }

        ManagementStrategy managementStrategy = camelContext.getManagementStrategy();

        if (configuration.isEnableRouteEventNotifier()) {
            MicrometerRouteEventNotifier notifier = new MicrometerRouteEventNotifier();
            notifier.setMeterRegistry(meterRegistry);
            managementStrategy.addEventNotifier(notifier);
        }
    }

}
