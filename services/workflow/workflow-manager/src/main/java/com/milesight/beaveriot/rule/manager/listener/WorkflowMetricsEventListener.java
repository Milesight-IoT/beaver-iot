package com.milesight.beaveriot.rule.manager.listener;

import com.milesight.beaveriot.base.constants.MetricsConstants;
import com.milesight.beaveriot.context.integration.model.event.MetricsEvent;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.manager.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
@Slf4j
@Component
public class WorkflowMetricsEventListener {

    @Autowired
    private WorkflowService workflowService;

    @EventListener(condition = "#p0.metricsName == '" + MetricsConstants.METRICS_EXCHANGE_EXECUTION_REPEAT_MAX + "'")
    public void onExceedMaxExchangeRepeat(MetricsEvent metricsEvent) {

        log.error("Exceed max exchange repeat times, metricsEvent: {}", metricsEvent);
        Map<String, String> tags = metricsEvent.getTags();
        String flowId = tags.get(MetricsConstants.ROUTE_ID_TAG);
        if (!ObjectUtils.isEmpty(flowId) && NumberUtils.isCreatable(flowId)) {
            workflowService.updateStatus(Long.valueOf(flowId), false);
        }

    }


}
