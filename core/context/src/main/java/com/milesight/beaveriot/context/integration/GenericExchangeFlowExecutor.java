package com.milesight.beaveriot.context.integration;

import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
@Slf4j
public class GenericExchangeFlowExecutor {

    private RuleEngineExecutor ruleEngineExecutor;

    public GenericExchangeFlowExecutor(RuleEngineExecutor ruleEngineExecutor) {
        this.ruleEngineExecutor = ruleEngineExecutor;
    }

    public EventResponse saveValuesAndPublishSync(ExchangePayload exchangePayload, String eventType) {
        if (ObjectUtils.isEmpty(exchangePayload)) {
            log.error("ExchangePayload is empty when saveValuesAndPublish");
            return EventResponse.empty();
        }

        Map<EntityType, ExchangePayload> splitExchangePayloads = exchangePayload.splitExchangePayloads();
        EventResponse eventResponse = EventResponse.empty();
        splitExchangePayloads.forEach((entityType, payload) -> {
            initializeEventContext(eventType, entityType, payload, true);
            Object response = ruleEngineExecutor.executeWithResponse(RuleNodeNames.innerExchangeFlow, EntityValueType.convertValue(payload), ExchangeContextHelper.getTransmitCamelContext(payload.getContext()));
            if (response != null) {
                if (!(response instanceof Map returnEvent)) {
                    log.warn("Synchronous call result response is not a Map, response:{}", response);
                } else {
                    eventResponse.putAll(returnEvent);
                }
            }
        });
        return eventResponse;
    }

    public EventResponse saveValuesAndPublishSync(ExchangePayload exchangePayload) {
        return saveValuesAndPublishSync(exchangePayload, "");
    }

    public void saveValuesAndPublishAsync(ExchangePayload exchangePayload) {
        saveValuesAndPublishAsync(exchangePayload, "");
    }

    public void saveValuesAndPublishAsync(ExchangePayload exchangePayload, String eventType) {
        if (ObjectUtils.isEmpty(exchangePayload)) {
            log.error("ExchangePayload is empty when saveValuesAndPublish");
            return;
        }

        Map<EntityType, ExchangePayload> splitExchangePayloads = exchangePayload.splitExchangePayloads();
        splitExchangePayloads.forEach((entityType, payload) -> {
            initializeEventContext(eventType, entityType, payload, false);
            ruleEngineExecutor.execute(RuleNodeNames.innerExchangeFlow, EntityValueType.convertValue(payload));
        });
    }

    protected void initializeEventContext(@Nullable String eventType, EntityType entityType, ExchangePayload payload, boolean syncCall) {
        ExchangeContextHelper.initializeCallMod(payload, syncCall);
        ExchangeContextHelper.initializeEventType(payload, ExchangeEvent.EventType.of(entityType, eventType));
        ExchangeContextHelper.initializeExchangeContext(payload);
    }

}
