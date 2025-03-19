package com.milesight.beaveriot.context.integration.model.event;

import lombok.Data;

import java.util.Map;

/**
 * @author leon
 */
@Data
public class MetricsEvent {

    private String metricsName;

    private Double metricsValue;

    private Map<String,String> tags;

    public static MetricsEvent of(String metricsName, Double metricsValue, Map<String,String> tags) {
        MetricsEvent metricsEvent = new MetricsEvent();
        metricsEvent.setMetricsName(metricsName);
        metricsEvent.setMetricsValue(metricsValue);
        metricsEvent.setTags(tags);
        return metricsEvent;
    }

    public static MetricsEvent of(String metricsName, Double metricsValue) {
        MetricsEvent metricsEvent = new MetricsEvent();
        metricsEvent.setMetricsName(metricsName);
        metricsEvent.setMetricsValue(metricsValue);
        return metricsEvent;
    }

}
