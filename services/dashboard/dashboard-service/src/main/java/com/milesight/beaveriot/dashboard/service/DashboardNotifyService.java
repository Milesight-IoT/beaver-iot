package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.model.event.MqttEvent;
import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
import com.milesight.beaveriot.dashboard.model.DashboardExchangePayload;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.mqtt.api.MqttAdminPubSubServiceProvider;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author loong
 * @date 2024/10/18 11:15
 */
@Service
@Slf4j
public class DashboardNotifyService {

    @Autowired
    private EntityServiceProvider entityServiceProvider;

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
            List<String> entityIds = entityServiceProvider.findByKeys(entityKeys)
                    .values()
                    .stream()
                    .map(Entity::getId)
                    .map(String::valueOf)
                    .toList();

            DashboardExchangePayload dashboardExchangePayload = new DashboardExchangePayload(entityIds);
            String event = JsonUtils.toJSON(MqttEvent.of(MqttEvent.EventType.EXCHANGE, dashboardExchangePayload));
            String webMqttUsername = MqttAdminPubSubServiceProvider.getWebUsername(tenantId);
            mqttPubSubServiceProvider.publish(webMqttUsername, "downlink/web/exchange",
                    event.getBytes(StandardCharsets.UTF_8), MqttQos.AT_MOST_ONCE, false);

            log.info("onDashboardNotify:{}", exchangePayload);
        } catch (Exception e) {
            log.error("onDashboardNotify error:{}", e.getMessage(), e);
        }
    }

}
