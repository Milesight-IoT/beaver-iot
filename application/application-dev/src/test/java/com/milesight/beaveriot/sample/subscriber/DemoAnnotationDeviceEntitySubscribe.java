package com.milesight.beaveriot.sample.subscriber;


import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.sample.entity.DemoDeviceEntities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */
@Component
@Slf4j
public class DemoAnnotationDeviceEntitySubscribe {
    @EventSubscribe(payloadKeyExpression ="demo-anno-integration.device.*.temperature", eventType = ExchangeEvent.EventType.DOWN)
    public void subscribeTemperature(Event<DemoDeviceEntities> event) {
        DemoDeviceEntities payload = event.getPayload();
        Double temperature = payload.getTemperature();
        log.info("DemoAnnotationDeviceEntitySubscribe subscribeTemperature:{}, temperature: {}",event,temperature);
    }

    @EventSubscribe(payloadKeyExpression ="demo-anno-integration.device.demoSN.*", eventType = ExchangeEvent.EventType.DOWN)
    public void subscribeDeviceProperties(Event<DemoDeviceEntities> event) {
        DemoDeviceEntities payload = event.getPayload();
        String changeStatus = payload.getChangeStatus();
        log.info("DemoAnnotationDeviceEntitySubscribe subscribeDeviceProperties:{}, status:{} ",event, changeStatus);
    }

    @EventSubscribe(payloadKeyExpression ="demo-anno-integration.integration.syncHistory")
    public void subscribeMsc(ExchangeEvent event) {
        log.info("DemoAnnotationDeviceEntitySubscribe subscribeMsc:{}",event);
    }

}
