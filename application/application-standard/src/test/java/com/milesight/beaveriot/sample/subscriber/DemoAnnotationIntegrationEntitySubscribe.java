package com.milesight.beaveriot.sample.subscriber;


import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.sample.entity.DemoIntegrationEntities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */
@Component
@Slf4j
public class DemoAnnotationIntegrationEntitySubscribe {
    @EventSubscribe(payloadKeyExpression="demo-anno-integration.integration.connect.*")
    public EventResponse onConnect(Event<DemoIntegrationEntities.DemoGroupSettingEntities> event) {
        log.debug("DemoAnnotationIntegrationEntitySubscribe onConnect, AccessKey :{}, SecretKey {}",event.getPayload().getAccessKey(),event.getPayload().getSecretKey());
        return EventResponse.of("connectResult",event.getPayload().getAllPayloads());
    }

    @EventSubscribe(payloadKeyExpression="demo-anno-integration.integration.connect.deviceSync")
    public void onDeviceSync(ExchangeEvent event) {
        log.debug("DemoAnnotationIntegrationEntitySubscribe onDeviceSync:{}",event);
    }

    @EventSubscribe(payloadKeyExpression="demo-anno-integration.integration.connect.entitySync")
    public EventResponse onEntitySync(Event<DemoIntegrationEntities> event) {
        log.debug("DemoAnnotationIntegrationEntitySubscribe onEntitySync:{}",event);
        return EventResponse.of("syncEntitySize",event.getPayload().getAllPayloads().size());
    }


}
