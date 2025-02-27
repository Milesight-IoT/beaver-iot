package com.milesight.beaveriot.sample.subscriber;


import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.sample.entity.DemoDeviceEntities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */
@Component
@Slf4j
public class DemoAnnotationDeviceEntitySubscribe {
    @Async
    @EventSubscribe(payloadKeyExpression ="demo-anno-integration.device.*.temperature", eventType = {ExchangeEvent.EventType.REPORT_EVENT})
    public void subscribeTemperature(Event<DemoDeviceEntities> event) {
        DemoDeviceEntities payload = event.getPayload();
        Double temperature = payload.getTemperature();
        log.info("DemoAnnotationDeviceEntitySubscribe subscribeTemperature:{}, temperature: {}",event,temperature);
    }

    @EventSubscribe(payloadKeyExpression ="demo-anno-integration.device.demoSN.*")
    public void subscribeDeviceProperties(Event<DemoDeviceEntities> event) {
        DemoDeviceEntities payload = event.getPayload();
        String changeStatus = payload.getChangeStatus();
        log.info("DemoAnnotationDeviceEntitySubscribe subscribeDeviceProperties:{}, status:{} ",event, changeStatus);
    }

    @Async
    @EventSubscribe(payloadKeyExpression ="demo-anno-integration.integration.connect.*")
    public void subscribeMsc(ExchangeEvent event) {
        log.info("DemoAnnotationDeviceEntitySubscribe subscribeMsc:{}",event);
    }

}
