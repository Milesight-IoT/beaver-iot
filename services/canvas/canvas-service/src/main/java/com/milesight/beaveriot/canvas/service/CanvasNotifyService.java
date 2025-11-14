package com.milesight.beaveriot.canvas.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.canvas.model.dto.CanvasExchangePayload;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.model.event.MqttEvent;
import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
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
public class CanvasNotifyService {
    @Autowired
    private MqttPubSubServiceProvider mqttPubSubServiceProvider;

    @EventSubscribe(payloadKeyExpression = "*")
    public void onCanvasNotify(ExchangeEvent exchangeEvent) {
        doCanvasNotify(exchangeEvent.getPayload());
    }

    private void doCanvasNotify(ExchangePayload exchangePayload) {
        try {
            String tenantId = (String) exchangePayload.getContext(ExchangeContextKeys.SOURCE_TENANT_ID);
            if (!StringUtils.hasText(tenantId)) {
                throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
            }
            List<String> entityIds = exchangePayload.getExchangeEntities()
                    .values()
                    .stream()
                    .filter(entity -> EntityType.PROPERTY.equals(entity.getType()))
                    .map(Entity::getId)
                    .map(String::valueOf)
                    .toList();

            if (entityIds.isEmpty()) {
                return;
            }

            CanvasExchangePayload canvasExchangePayload = new CanvasExchangePayload(entityIds);
            String event = JsonUtils.toJSON(MqttEvent.of(MqttEvent.EventType.EXCHANGE, canvasExchangePayload));
            String webMqttUsername = MqttAdminPubSubServiceProvider.getWebUsername(tenantId);
            mqttPubSubServiceProvider.publish(webMqttUsername, "downlink/web/exchange",
                    event.getBytes(StandardCharsets.UTF_8), MqttQos.AT_MOST_ONCE, false);

            log.debug("onCanvasNotify:{}", canvasExchangePayload);
        } catch (Exception e) {
            log.error("onCanvasNotify error:{}", e.getMessage(), e);
        }
    }

}
