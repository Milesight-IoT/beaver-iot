package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.model.event.WebSocketEvent;
import com.milesight.beaveriot.dashboard.context.DashboardWebSocketContext;
import com.milesight.beaveriot.dashboard.model.DashboardExchangePayload;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.websocket.WebSocketContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author loong
 * @date 2024/10/18 11:15
 */
@Service
@Slf4j
public class DashboardNotifyService {

    @EventSubscribe(payloadKeyExpression = "*")
    public void onDeviceDashboardNotify(ExchangeEvent exchangeEvent) {
        doDashboardNotify(exchangeEvent.getPayload());
    }

    private void doDashboardNotify(ExchangePayload exchangePayload) {
        try {
            //TODO Ensure the presence of tenantId in the context
            Long tenantId = UserConstants.DEFAULT_TENANT_ID;
            List<String> entityKeys = exchangePayload.keySet().stream().toList();
            List<String> keys = DashboardWebSocketContext.getKeysByValues(entityKeys);
            if (keys == null || keys.isEmpty()) {
                return;
            }
            List<String> sendKeys = new ArrayList<>();
            keys.forEach(key -> {
                String key_tenantId = key.split(WebSocketContext.KEY_JOIN_SYMBOL)[0];
                if (key_tenantId.equals(tenantId.toString())) {
                    sendKeys.add(key);
                }
            });
            if (sendKeys.isEmpty()) {
                return;
            }
            DashboardExchangePayload dashboardExchangePayload = new DashboardExchangePayload();
            dashboardExchangePayload.setEntityKey(entityKeys);
            WebSocketEvent webSocketEvent = WebSocketEvent.of(WebSocketEvent.EventType.EXCHANGE, dashboardExchangePayload);
            sendKeys.forEach(key -> WebSocketContext.sendMessage(key, JsonUtils.toJSON(webSocketEvent)));
            log.info("onDashboardNotify:{}", exchangePayload);
        } catch (Exception e) {
            log.error("onDashboardNotify error:{}", e.getMessage(), e);
        }
    }

}
