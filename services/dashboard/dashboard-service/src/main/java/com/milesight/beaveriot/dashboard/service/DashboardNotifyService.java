package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.model.event.WebSocketEvent;
import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
import com.milesight.beaveriot.dashboard.context.DashboardWebSocketContext;
import com.milesight.beaveriot.dashboard.model.DashboardExchangePayload;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.mqtt.api.MqttAdminPubSubServiceProvider;
import com.milesight.beaveriot.websocket.WebSocketContext;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author loong
 * @date 2024/10/18 11:15
 */
@Service
@Slf4j
public class DashboardNotifyService {

    @Autowired
    private MqttPubSubServiceProvider mqttPubSubServiceProvider;

    @EventSubscribe(payloadKeyExpression = "*")
    public void onDeviceDashboardNotify(ExchangeEvent exchangeEvent) {
        doDashboardNotify(exchangeEvent.getPayload());
    }

    private void doDashboardNotify(ExchangePayload exchangePayload) {
        try {
            String tenantId = (String) exchangePayload.getContext(ExchangeContextKeys.SOURCE_TENANT_ID);
            if (!StringUtils.hasText(tenantId)) {
                throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
            }
            List<String> entityKeys = exchangePayload.keySet().stream().toList();
            List<String> keys = DashboardWebSocketContext.getKeysByValues(entityKeys);
            if (keys == null || keys.isEmpty()) {
                return;
            }
            List<String> sendKeys = new ArrayList<>();
            keys.forEach(key -> {
                String key_tenantId = key.split(WebSocketContext.KEY_JOIN_SYMBOL)[0];
                if (key_tenantId.equals(tenantId)) {
                    sendKeys.add(key);
                }
            });
            if (sendKeys.isEmpty()) {
                return;
            }
            DashboardExchangePayload dashboardExchangePayload = new DashboardExchangePayload();
            dashboardExchangePayload.setEntityKey(entityKeys);
            String webSocketEvent = JsonUtils.toJSON(WebSocketEvent.of(WebSocketEvent.EventType.EXCHANGE, dashboardExchangePayload));
            sendKeys.forEach(key -> WebSocketContext.sendMessage(key, webSocketEvent));

            String webMqttUsername = MqttAdminPubSubServiceProvider.getWebUsername(tenantId);
            mqttPubSubServiceProvider.publish(webMqttUsername, "app/dashboard", webSocketEvent.getBytes(StandardCharsets.UTF_8), MqttQos.AT_MOST_ONCE, false);

            log.info("onDashboardNotify:{}", exchangePayload);
        } catch (Exception e) {
            log.error("onDashboardNotify error:{}", e.getMessage(), e);
        }
    }

}
