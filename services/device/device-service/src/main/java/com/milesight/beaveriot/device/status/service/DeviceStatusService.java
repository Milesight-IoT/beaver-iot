package com.milesight.beaveriot.device.status.service;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.context.integration.model.DeviceStatusConfig;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/4 11:02
 **/
@Service
public class DeviceStatusService {
    private final DeviceStatusManager deviceStatusManager;

    public DeviceStatusService(DeviceStatusManager deviceStatusManager) {
        this.deviceStatusManager = deviceStatusManager;
    }

    public void register(String integrationId, DeviceStatusConfig config) {
        deviceStatusManager.register(integrationId, config);
    }

    public void deregister(Device device) {
        deviceStatusManager.deregister(device);
    }

    public void online(Device device) {
        deviceStatusManager.online(device);
    }

    public void offline(Device device) {
        deviceStatusManager.offline(device);
    }

    public DeviceStatus status(Device device) {
        return deviceStatusManager.status(device);
    }

    public Map<String, DeviceStatus> getStatusesByDeviceKeys(List<String> deviceKeys) {
        return deviceStatusManager.getStatusesByDeviceKeys(deviceKeys);
    }
}