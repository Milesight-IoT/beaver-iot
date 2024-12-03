package com.milesight.beaveriot.device.facade;

import com.milesight.beaveriot.device.dto.DeviceNameDTO;

import java.util.List;

public interface IDeviceFacade {
    List<DeviceNameDTO> fuzzySearchDeviceByName(String name);

    List<DeviceNameDTO> getDeviceNameByIntegrations(List<String> integrationIds);

    List<DeviceNameDTO> getDeviceNameByIds(List<Long> deviceIds);

    List<DeviceNameDTO> getDeviceNameByKey(List<String> deviceKeys);

    DeviceNameDTO getDeviceNameByKey(String deviceKey);
}
