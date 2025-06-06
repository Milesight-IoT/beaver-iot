package com.milesight.beaveriot.metrics.camel;

import com.milesight.beaveriot.base.exception.CyclicCallException;
import com.milesight.beaveriot.context.integration.model.event.MetricsEvent;
import com.milesight.beaveriot.metrics.autoconfigure.CamelMetricsConfiguration;
import com.milesight.beaveriot.base.constants.MetricsConstants;
import com.milesight.beaveriot.metrics.utils.TagUtils;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import io.micrometer.core.instrument.*;
import lombok.Getter;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyConfiguration;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy;
import org.apache.camel.spi.InflightRepository;
import org.apache.camel.util.ObjectHelper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.camel.component.micrometer.MicrometerConstants.DEFAULT_CAMEL_ROUTES_EXCHANGES_INFLIGHT;
import static org.apache.camel.component.micrometer.MicrometerConstants.DEFAULT_CAMEL_ROUTE_POLICY_METER_NAME;

@Getter
public final class MetricsStatistics {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final InflightRepository inflightRepository;
    private final MeterRegistry meterRegistry;
    private final Route route;
    private final MicrometerRoutePolicyNamingStrategy namingStrategy;
    private final MicrometerRoutePolicyConfiguration configuration;
    private Counter exchangesSucceeded;
    private Counter exchangesFailed;
    private Counter exchangesTotal;
    private Gauge exchangesInflight;
    private Timer timer;
    private LongTaskTimer longTaskTimer;
    private boolean isInnerRoute;
    private CamelMetricsConfiguration camelMetricsConfiguration;
    public MetricsStatistics(InflightRepository inflightRepository, ApplicationEventPublisher applicationEventPublisher,MeterRegistry meterRegistry, Route route,
                             MicrometerRoutePolicyNamingStrategy namingStrategy,
                             MicrometerRoutePolicyConfiguration configuration,
                             CamelMetricsConfiguration camelMetricsConfiguration) {
        this.configuration = ObjectHelper.notNull(configuration, "MicrometerRoutePolicyConfiguration", this);
        this.meterRegistry = ObjectHelper.notNull(meterRegistry, "MeterRegistry", this);
        this.namingStrategy = ObjectHelper.notNull(namingStrategy, "MicrometerRoutePolicyNamingStrategy", this);
        this.route = route;
        this.inflightRepository = inflightRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.camelMetricsConfiguration = camelMetricsConfiguration;
        if (configuration.isAdditionalCounters()) {
            initAdditionalCounters();
        }
        isInnerRoute = route.getRouteId().startsWith(RuleNodeNames.innerExchangeRouteId);
    }
    private Map<String, Integer> executionRepeatMaxCounts = new LinkedHashMap<>();

    private void initAdditionalCounters() {
        if (configuration.isExchangesSucceeded()) {
            this.exchangesSucceeded = createCounter(namingStrategy.getExchangesSucceededName(route),
                    "Number of successfully completed exchanges");
        }
        if (configuration.isExchangesFailed()) {
            this.exchangesFailed
                    = createCounter(namingStrategy.getExchangesFailedName(route), "Number of failed exchanges");
        }
        if (configuration.isExchangesTotal()) {
            this.exchangesTotal
                    = createCounter(namingStrategy.getExchangesTotalName(route), "Total number of processed exchanges");
        }
        this.exchangesInflight = createGauge(getExchangesInflightName(route), "Number of exchanges inflight");
        if (configuration.isLongTask()) {
            LongTaskTimer.Builder builder = LongTaskTimer.builder(namingStrategy.getLongTaskName(route))
                    .tags(namingStrategy.getTags(route))
                    .description("Route long task metric");
            if (configuration.getLongTaskInitiator() != null) {
                configuration.getLongTaskInitiator().accept(builder);
            }
            longTaskTimer = builder.register(meterRegistry);
        }
    }

    private String getExchangesInflightName(Route route) {
        return DEFAULT_CAMEL_ROUTES_EXCHANGES_INFLIGHT;
    }

    public void onExchangeBegin(Exchange exchange) {
        Timer.Sample sample = Timer.start(meterRegistry);
        exchange.setProperty(propertyName(exchange), sample);
        if (longTaskTimer != null) {
            exchange.setProperty(propertyName(exchange) + "_long_task", longTaskTimer.start());
        }

        // add exchange execution count
        if (!isInnerRoute) {
            updateExecutionMaxCount(exchange);
        }
    }

    private void updateExecutionMaxCount(Exchange exchange) {
        String rootFlowId = exchange.getProperty(ExchangeHeaders.EXCHANGE_ROOT_FLOW_ID, exchange.getFromRouteId(), String.class);
        AtomicInteger repeatCount = (AtomicInteger) exchange.getProperty(ExchangeHeaders.EXCHANGE_EXECUTION_REPEAT_COUNT);
        if (repeatCount == null ) {
            repeatCount = new AtomicInteger(0);
            exchange.setProperty(ExchangeHeaders.EXCHANGE_EXECUTION_REPEAT_COUNT, repeatCount);
        }

        // update execution repeat max count， if repeat count > 1 report metrics
        if (resetExecutionRepeatMaxCountIfNecessary(rootFlowId, repeatCount.incrementAndGet()) && repeatCount.get() > 1) {
            Tags tags = namingStrategy.getExchangeStatusTags(route)
                    .and(MetricsConstants.ROUTE_NAME_TAG, getTagValue(route.getDescription(), MetricsConstants.DEFAULT_NONE_VALUE))
                    .and(MetricsConstants.ROUTE_ROOT_ROUTE_ID, getTagValue(rootFlowId, MetricsConstants.DEFAULT_NONE_VALUE));
            Metrics.gauge(MetricsConstants.METRICS_EXCHANGE_EXECUTION_REPEAT_MAX,tags.stream().toList(),
                    executionRepeatMaxCounts,  map -> map.getOrDefault(rootFlowId, 0));
            if (repeatCount.get() >= camelMetricsConfiguration.getThresholdConfig().getExchangeRepeatMax()) {
                exchangesTotal.increment();
                exchangesFailed.increment();
                exchange.setException(new CyclicCallException("The number of exchanges exceeds the maximum number of times, check whether there are loop calls in the flow" ));
            }
        }
    }

    private boolean resetExecutionRepeatMaxCountIfNecessary(String rootFlowId,  int maxCount) {
        AtomicBoolean reset = new AtomicBoolean(true);
        if (executionRepeatMaxCounts.containsKey(rootFlowId)) {
            executionRepeatMaxCounts.computeIfPresent(rootFlowId, (String, value) -> {
                reset.set(maxCount > value);
                return Math.max(value, maxCount);
            });

        } else {
            executionRepeatMaxCounts.put(rootFlowId, maxCount);
        }
        return reset.get();
    }

    public void onExchangeDone(Exchange exchange) {
        Boolean hasExchangeDone = exchange.getProperty(ExchangeHeaders.EXCHANGE_DONE_ADVICE_FLAG, false, Boolean.class);
        if (hasExchangeDone) {
            return;
        }
        exchange.setProperty(ExchangeHeaders.EXCHANGE_DONE_ADVICE_FLAG, true);

        Timer.Sample sample = (Timer.Sample) exchange.removeProperty(propertyName(exchange));
        if (sample != null) {
            if (timer == null) {
                Timer.Builder builder = Timer.builder(namingStrategy.getName(route))
                        .tags(namingStrategy.getTags(route).and(MetricsConstants.ROUTE_NAME_TAG, getTagValue(route.getDescription(), MetricsConstants.DEFAULT_NONE_VALUE)))
                        .description("Route performance metrics");
                if (configuration.getTimerInitiator() != null) {
                    configuration.getTimerInitiator().accept(builder);
                }
                timer = builder.register(meterRegistry);
            }
            sample.stop(timer);
        }
        LongTaskTimer.Sample ltSampler
                = (LongTaskTimer.Sample) exchange.removeProperty(propertyName(exchange) + "_long_task");
        if (ltSampler != null) {
            ltSampler.stop();
        }
        if (configuration.isAdditionalCounters()) {
            updateAdditionalCounters(exchange);
        }
    }

    private void updateAdditionalCounters(Exchange exchange) {
        if (exchangesTotal != null) {
            exchangesTotal.increment();
        }
        if (exchange.isFailed()) {
            if (exchangesFailed != null) {
                exchangesFailed.increment();
            }
        } else {
            if (exchangesSucceeded != null) {
                exchangesSucceeded.increment();
            }
        }
    }

    private String propertyName(Exchange exchange) {
        return String.format("%s-%s-%s", DEFAULT_CAMEL_ROUTE_POLICY_METER_NAME, route.getId(), exchange.getExchangeId());
    }

    private Counter createCounter(String meterName, String description) {
        return Counter.builder(meterName)
                .tags(namingStrategy.getExchangeStatusTags(route).and(MetricsConstants.ROUTE_NAME_TAG, getTagValue(route.getDescription(), MetricsConstants.DEFAULT_NONE_VALUE)))
                .description(description)
                .register(meterRegistry);
    }

    private String getTagValue(String value, String defaultNoneValue) {
        return ObjectUtils.isEmpty(value) ? defaultNoneValue : value;
    }

    private Gauge createGauge(String meterName, String description) {
        return Gauge.builder(meterName, () -> inflightRepository.size(route.getRouteId()))
                .tags(namingStrategy.getTags(route).and(MetricsConstants.ROUTE_NAME_TAG, getTagValue(route.getDescription(), MetricsConstants.DEFAULT_NONE_VALUE)))
                .description(description)
                .register(meterRegistry);
    }
}