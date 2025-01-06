package com.milesight.beaveriot.context.integration;

import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.eventbus.enums.EventSource;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.*;

/**
 * @author leon
 */
@Slf4j
public class GenericExchangeFlowExecutor implements ExchangeFlowExecutor {

    private RuleEngineExecutor ruleEngineExecutor;

    public GenericExchangeFlowExecutor(RuleEngineExecutor ruleEngineExecutor) {
        this.ruleEngineExecutor = ruleEngineExecutor;
    }

    @Override
    public EventResponse syncExchange(ExchangePayload exchangePayload, EventSource eventSource) {
        initializeEventContext(eventSource, exchangePayload, true);
        Object response = ruleEngineExecutor.executeWithResponse(RuleNodeNames.innerExchangeFlow, exchangePayload);
        if (response != null && !(response instanceof EventResponse)) {
            log.warn("syncExchangeDown response is not EventResponse, response:{}", response);
            return EventResponse.empty();
        } else {
            return (EventResponse) response;
        }
    }

    @Override
    public void asyncExchange(ExchangePayload exchangePayload, EventSource eventSource) {
        initializeEventContext(eventSource, exchangePayload, false);
        ruleEngineExecutor.execute(RuleNodeNames.innerExchangeFlow, exchangePayload);
    }

    @Override
    public void asyncExchange(ExchangePayload exchangePayload) {
        asyncExchange(exchangePayload, null);
    }

    @Override
    public EventResponse syncExchange(ExchangePayload exchangePayload) {
        return syncExchange(exchangePayload, null);
    }

    protected void initializeEventContext(@Nullable EventSource eventSource, ExchangePayload payload, boolean syncCall) {
        payload.putContext(EXCHANGE_SYNC_CALL, syncCall);
        payload.putContext(EXCHANGE_EVENT_TYPE, ExchangeEvent.EventType.TRANSMIT);
        if (eventSource != null) {
            payload.putContext(EXCHANGE_EVENT_SOURCE, eventSource);
        }
        ExchangeContextHelper.initializeEventSource(payload);
    }

}
