package com.milesight.beaveriot.context.util;

import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.eventbus.enums.EventSource;
import org.apache.camel.Exchange;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * @author leon
 */
public class ExchangeContextHelper {

    private ExchangeContextHelper() {
    }

    public static void initializeEventSource(ExchangePayload exchangePayload) {
        initializeEventSource(exchangePayload, Map.of());
    }

    public static void initializeEventSource(ExchangePayload exchangePayload, Exchange exchange) {
        initializeEventSource(exchangePayload, exchange.getProperties());
    }

    public static void initializeEventSource(ExchangePayload exchangePayload, Map<String,Object> context) {
        Assert.notNull(context, "headers must not be null");

        // set source user id, tenant id, flow id in order
        Long userId = (Long) context.getOrDefault(ExchangeContextKeys.SOURCE_USER_ID, SecurityUserContext.getUserId());
        Long tenantId = (Long) context.getOrDefault(ExchangeContextKeys.SOURCE_TENANT_ID, SecurityUserContext.getTenantId());
        Serializable flowId = (Serializable) context.get(ExchangeContextKeys.SOURCE_FLOW_ID);
        putContextIfNecessary(exchangePayload, ExchangeContextKeys.SOURCE_USER_ID, userId);
        putContextIfNecessary(exchangePayload, ExchangeContextKeys.SOURCE_TENANT_ID, tenantId);
        putContextIfNecessary(exchangePayload, ExchangeContextKeys.SOURCE_FLOW_ID, flowId);

        // set event source
        if (!exchangePayload.containsKey(ExchangeContextKeys.EXCHANGE_EVENT_SOURCE)) {
            if (userId != null) {
                exchangePayload.putContext(ExchangeContextKeys.EXCHANGE_EVENT_SOURCE, EventSource.USER);
            } else if (flowId != null) {
                exchangePayload.putContext(ExchangeContextKeys.EXCHANGE_EVENT_SOURCE, EventSource.WORKFLOW);
            } else {
                exchangePayload.putContext(ExchangeContextKeys.EXCHANGE_EVENT_SOURCE, EventSource.INTEGRATION);
            }
        }
    }

    private static void putContextIfNecessary(ExchangePayload exchangePayload, String key, Serializable value) {
        if (exchangePayload.containsKey(key)) {
            return;
        }
        if (!ObjectUtils.isEmpty(value)) {
            exchangePayload.putContext(key, value);
        }
    }

}
