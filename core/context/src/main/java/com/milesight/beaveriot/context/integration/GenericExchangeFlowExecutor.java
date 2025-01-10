package com.milesight.beaveriot.context.integration;

import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.EXCHANGE_EVENT_TYPE;
import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.EXCHANGE_SYNC_CALL;

/**
 * @author leon
 */
@Slf4j
public class GenericExchangeFlowExecutor {

    private RuleEngineExecutor ruleEngineExecutor;

    public GenericExchangeFlowExecutor(RuleEngineExecutor ruleEngineExecutor) {
        this.ruleEngineExecutor = ruleEngineExecutor;
    }

    public void saveValuesAndPublish(ExchangePayload exchangePayload, String eventType, Consumer<EventResponse> consumer) {
        if (ObjectUtils.isEmpty(exchangePayload)) {
            log.error("ExchangePayload is empty when saveValuesAndPublish");
            return;
        }

        Map<EntityType, ExchangePayload> splitExchangePayloads = exchangePayload.splitExchangePayloads();
        EventResponse eventResponse = EventResponse.empty();
        splitExchangePayloads.forEach((entityType, payload) -> {
            initializeEventContext(eventType, entityType, payload, true);
            Object response = ruleEngineExecutor.executeWithResponse(RuleNodeNames.innerExchangeFlow, payload);
            if (response != null) {
                if (!(response instanceof EventResponse returnEvent)) {
                    log.warn("syncExchangeDown response is not EventResponse, response:{}", response);
                } else {
                    eventResponse.putAll(returnEvent);
                }
            }
        });

        consumer.accept(eventResponse);
    }

    public void saveValuesAndPublish(ExchangePayload exchangePayload, Consumer<EventResponse> consumer) {
        saveValuesAndPublish(exchangePayload, "", consumer);
    }

    public void saveValuesAndPublish(ExchangePayload exchangePayload) {
        saveValuesAndPublish(exchangePayload, "");
    }

    public void saveValuesAndPublish(ExchangePayload exchangePayload, String eventType) {
        if (ObjectUtils.isEmpty(exchangePayload)) {
            log.error("ExchangePayload is empty when saveValuesAndPublish");
            return;
        }

        Map<EntityType, ExchangePayload> splitExchangePayloads = exchangePayload.splitExchangePayloads();
        splitExchangePayloads.forEach((entityType, payload) -> {
            initializeEventContext(eventType, entityType, payload, false);
            ruleEngineExecutor.execute(RuleNodeNames.innerExchangeFlow, payload);
        });
    }

    protected void initializeEventContext(@Nullable String eventType, EntityType entityType, ExchangePayload payload, boolean syncCall) {
        ExchangeContextHelper.initializeCallMod(payload, syncCall);
        ExchangeContextHelper.initializeEventType(payload, ExchangeEvent.EventType.of(entityType, eventType));
        ExchangeContextHelper.initializeEventSource(payload);
    }

}
