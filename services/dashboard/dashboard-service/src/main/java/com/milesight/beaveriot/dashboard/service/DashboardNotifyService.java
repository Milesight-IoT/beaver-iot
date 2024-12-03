package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.model.event.WebSocketEvent;
import com.milesight.beaveriot.dashboard.context.DashboardWebSocketContext;
import com.milesight.beaveriot.dashboard.model.DashboardExchangePayload;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.websocket.WebSocketContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            List<String> entityKeys = exchangePayload.keySet().stream().toList();
            List<String> userIds = DashboardWebSocketContext.getKeysByValues(entityKeys);
            if (userIds == null || userIds.isEmpty()) {
                return;
            }
            DashboardExchangePayload dashboardExchangePayload = new DashboardExchangePayload();
            dashboardExchangePayload.setEntityKey(entityKeys);
            WebSocketEvent webSocketEvent = WebSocketEvent.of(WebSocketEvent.EventType.EXCHANGE, dashboardExchangePayload);
            userIds.forEach(userId -> WebSocketContext.sendMessage(userId, JsonUtils.toJSON(webSocketEvent)));
            log.info("onDashboardNotify:{}", exchangePayload);
        } catch (Exception e) {
            log.error("onDashboardNotify error:{}", e.getMessage(), e);
        }
    }

}
