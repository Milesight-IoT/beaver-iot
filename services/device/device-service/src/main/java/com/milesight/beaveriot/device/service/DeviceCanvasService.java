package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.facade.ICanvasFacade;
import com.milesight.beaveriot.canvas.model.dto.CanvasDTO;
import com.milesight.beaveriot.device.model.response.DeviceCanvasResponse;
import com.milesight.beaveriot.device.po.DevicePO;
import com.milesight.beaveriot.device.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * DeviceCanvasService class.
 *
 * @author simon
 * @date 2025/9/11
 */
@Service
public class DeviceCanvasService {
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    ICanvasFacade canvasFacade;

    @DistributedLock(name = "device-canvas-create-#{#p0}", waitForLock = "5s")
    public DeviceCanvasResponse getOrCreateDeviceCanvas(Long deviceId) {
        Optional<DevicePO> findResult = deviceRepository.findByIdWithDataPermission(deviceId);
        if (findResult.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        DevicePO devicePO = findResult.get();
        String deviceIdStr = devicePO.getId().toString();
        List<CanvasDTO> canvasList = canvasFacade.getCanvasByAttach(CanvasAttachType.DEVICE, List.of(deviceIdStr));
        CanvasDTO deviceCanvas = canvasList.isEmpty() ? canvasFacade.createCanvas(devicePO.getName(), CanvasAttachType.DEVICE, deviceIdStr) : canvasList.get(0);
        DeviceCanvasResponse response = new DeviceCanvasResponse();
        response.setCanvasId(deviceCanvas.getId().toString());
        response.setName(deviceCanvas.getName());
        return response;
    }
}
