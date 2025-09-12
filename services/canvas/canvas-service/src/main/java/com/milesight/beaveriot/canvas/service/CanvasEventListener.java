package com.milesight.beaveriot.canvas.service;

import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.facade.ICanvasFacade;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CanvasEventListener class.
 *
 * @author simon
 * @date 2025/9/11
 */
@Component
public class CanvasEventListener {
    @Autowired
    ICanvasFacade canvasFacade;

    @EventSubscribe(payloadKeyExpression = "*", eventType = DeviceEvent.EventType.DELETED)
    public void onDeleteDevice(DeviceEvent event) {
        canvasFacade.deleteCanvasByAttach(CanvasAttachType.DEVICE, List.of(event.getPayload().getId().toString()));
    }
}
