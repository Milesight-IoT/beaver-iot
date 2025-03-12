package com.milesight.beaveriot.metrics.camel;

import com.milesight.beaveriot.metrics.autoconfigure.CamelMetricsConfiguration;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.NonManagedService;
import org.apache.camel.Route;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.micrometer.MicrometerUtils;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyConfiguration;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyService;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.support.RoutePolicySupport;
import org.apache.camel.support.service.ServiceHelper;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.component.micrometer.MicrometerConstants.METRICS_REGISTRY_NAME;
import static org.apache.camel.component.micrometer.MicrometerConstants.SERVICE_NAME;

/**
 * @author leon
 */
@Data
public class CamelMicrometerRoutePolicy extends RoutePolicySupport implements NonManagedService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final InflightRepository inflightRepository;
    private final CamelMetricsConfiguration camelMetricsConfiguration;
    private MeterRegistry meterRegistry;
    private boolean prettyPrint;
    private TimeUnit durationUnit = TimeUnit.MILLISECONDS;
    private MicrometerRoutePolicyNamingStrategy namingStrategy = MicrometerRoutePolicyNamingStrategy.DEFAULT;
    private MicrometerRoutePolicyConfiguration configuration = MicrometerRoutePolicyConfiguration.DEFAULT;

    private final Map<Route, MetricsStatistics> statisticsMap = new HashMap<>();

    public CamelMicrometerRoutePolicy(InflightRepository inflightRepository, ApplicationEventPublisher applicationEventPublisher, CamelMetricsConfiguration camelMetricsConfiguration) {
        this.inflightRepository = inflightRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.camelMetricsConfiguration = camelMetricsConfiguration;
    }

    @Override
    public void onInit(Route route) {
        super.onInit(route);

        if (getMeterRegistry() == null) {
            setMeterRegistry(MicrometerUtils.getOrCreateMeterRegistry(
                    route.getCamelContext().getRegistry(), METRICS_REGISTRY_NAME));
        }
        try {
            MicrometerRoutePolicyService registryService
                    = route.getCamelContext().hasService(MicrometerRoutePolicyService.class);
            if (registryService == null) {
                registryService = new MicrometerRoutePolicyService();
                registryService.setMeterRegistry(getMeterRegistry());
                registryService.setPrettyPrint(isPrettyPrint());
                registryService.setDurationUnit(getDurationUnit());
                registryService.setMatchingTags(Tags.of(SERVICE_NAME, MicrometerRoutePolicyService.class.getSimpleName()));
                route.getCamelContext().addService(registryService);
                ServiceHelper.startService(registryService);
            }
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    @Override
    public void onStart(Route route) {
        // create statistics holder
        // for now we record only all the timings of a complete exchange (responses)
        // we have in-flight / total statistics already from camel-core
        if (!shouldCollectStatistics(route)) {
            return;
        }
        statisticsMap.computeIfAbsent(route,
                it -> new MetricsStatistics(inflightRepository, applicationEventPublisher, getMeterRegistry(), it, getNamingStrategy(), configuration, camelMetricsConfiguration));
    }

    private boolean shouldCollectStatistics(Route route) {
        return !route.getRouteId().startsWith(RuleFlowIdGenerator.TRACER_FLOW_ID_PREFIX);
    }

    @Override
    public void onRemove(Route route) {
        if (!shouldCollectStatistics(route)) {
            return;
        }
        // route is removed, so remove metrics from micrometer
        MetricsStatistics stats = statisticsMap.remove(route);
        if (stats != null) {
            if (stats.getExchangesSucceeded() != null) {
                meterRegistry.remove(stats.getExchangesSucceeded());
            }
            if (stats.getExchangesFailed() != null) {
                meterRegistry.remove(stats.getExchangesFailed());
            }
            if (stats.getExchangesTotal() != null) {
                meterRegistry.remove(stats.getExchangesTotal());
            }
            if (stats.getTimer() != null) {
                meterRegistry.remove(stats.getTimer());
            }
            if (stats.getLongTaskTimer() != null) {
                meterRegistry.remove(stats.getLongTaskTimer());
            }
        }
    }

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        Optional.ofNullable(statisticsMap.get(route))
                .ifPresent(statistics -> statistics.onExchangeBegin(exchange));
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
        Optional.ofNullable(statisticsMap.get(route))
                .ifPresent(statistics -> statistics.onExchangeDone(exchange));
    }

}
