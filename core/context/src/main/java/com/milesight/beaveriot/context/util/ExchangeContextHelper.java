package com.milesight.beaveriot.context.util;

import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.SecurityUser;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import org.apache.camel.Exchange;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Map;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.EXCHANGE_EVENT_TYPE;
import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.EXCHANGE_SYNC_CALL;

/**
 * @author leon
 */
public class ExchangeContextHelper {

    private ExchangeContextHelper() {
    }

    public static void initializeCallMod(ExchangePayload exchangePayload, boolean syncCall) {
        exchangePayload.putContext(EXCHANGE_SYNC_CALL, syncCall);
    }

    public static void initializeEventType(ExchangePayload exchangePayload, String eventType) {
        exchangePayload.putContext(EXCHANGE_EVENT_TYPE, eventType);
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
        SecurityUser securityUser = (SecurityUser) context.getOrDefault(ExchangeContextKeys.SOURCE_USER, SecurityUserContext.getSecurityUser());
        Long tenantId = (Long) context.getOrDefault(ExchangeContextKeys.SOURCE_TENANT_ID, TenantContext.getTenantId());
        Serializable flowId = (Serializable) context.get(ExchangeContextKeys.SOURCE_FLOW_ID);
        putContextIfNecessary(exchangePayload, ExchangeContextKeys.SOURCE_USER, securityUser);
        putContextIfNecessary(exchangePayload, ExchangeContextKeys.SOURCE_TENANT_ID, tenantId);
        putContextIfNecessary(exchangePayload, ExchangeContextKeys.SOURCE_FLOW_ID, flowId);

    }

    private static void putContextIfNecessary(ExchangePayload exchangePayload, String key, Serializable value) {
        if (exchangePayload.getContext().containsKey(key)) {
            return;
        }
        if (!ObjectUtils.isEmpty(value)) {
            exchangePayload.putContext(key, value);
        }
    }
}
