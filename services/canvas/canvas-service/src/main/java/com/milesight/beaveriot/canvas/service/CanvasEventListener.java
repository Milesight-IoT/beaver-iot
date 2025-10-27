package com.milesight.beaveriot.canvas.service;

import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.facade.ICanvasFacade;
import com.milesight.beaveriot.canvas.po.CanvasPO;
import com.milesight.beaveriot.canvas.repository.CanvasRepository;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.dashboard.event.DashboardEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

    @Autowired
    CanvasRepository canvasRepository;


    @EventSubscribe(payloadKeyExpression = "*", eventType = DeviceEvent.EventType.DELETED)
    public void onDeleteDevice(DeviceEvent event) {
        canvasFacade.deleteCanvasByAttach(CanvasAttachType.DEVICE, List.of(event.getPayload().getId().toString()));
    }

    @EventSubscribe(payloadKeyExpression = "*", eventType = DeviceEvent.EventType.UPDATED)
    public void onUpdateDevice(DeviceEvent event) {
        canvasRepository.findOne(f -> f
                .eq(CanvasPO.Fields.attachType, CanvasAttachType.DEVICE)
                .eq(CanvasPO.Fields.attachId, event.getPayload().getId())
        ).ifPresent(canvasPO -> {
            canvasPO.setName(event.getPayload().getName());
            canvasRepository.save(canvasPO);
        });
    }

    @EventSubscribe(payloadKeyExpression = "*", eventType = DashboardEvent.EventType.UPDATED)
    public void onUpdateDashboard(DashboardEvent event) {
        canvasRepository.findOne(f -> f
                .eq(CanvasPO.Fields.attachType, CanvasAttachType.DASHBOARD)
                .eq(CanvasPO.Fields.attachId, event.getPayload().getDashboardId())
        ).ifPresent(canvasPO -> {
            canvasPO.setName(event.getPayload().getDashboardName());
            canvasRepository.save(canvasPO);
        });
    }
}
