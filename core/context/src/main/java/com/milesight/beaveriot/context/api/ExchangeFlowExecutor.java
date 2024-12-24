package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.eventbus.enums.EventSource;

/**
 * @author leon
 */
public interface ExchangeFlowExecutor {

    /**
     * sync exchange and return EventResponse, default event source is EventSource.INTEGRATION
     * @param exchangePayload
     * @return
     */
    EventResponse syncExchange(ExchangePayload exchangePayload);

    /**
     * async exchange, default event source is EventSource.INTEGRATION
     * @param exchangePayload
     */
    void asyncExchange(ExchangePayload exchangePayload);

    /**
     * sync exchange and assign EventSource
     * @param exchangePayload
     * @param eventSource
     * @return
     */
    EventResponse syncExchange(ExchangePayload exchangePayload, EventSource eventSource);

    /**
     * async exchange and assign EventSource
     * @param exchangePayload
     * @param eventSource
     */
    void asyncExchange(ExchangePayload exchangePayload, EventSource eventSource);

}
