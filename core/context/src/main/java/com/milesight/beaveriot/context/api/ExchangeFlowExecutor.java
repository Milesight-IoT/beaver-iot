package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;

/**
 * @author leon
 */
public interface ExchangeFlowExecutor {

    EventResponse syncExchangeUp(ExchangePayload exchangeEvent);

    void asyncExchangeUp(ExchangePayload exchangeEvent);

    EventResponse syncExchangeDown(ExchangePayload exchangeEvent);

    void asyncExchangeDown(ExchangePayload exchangeEvent);

}
