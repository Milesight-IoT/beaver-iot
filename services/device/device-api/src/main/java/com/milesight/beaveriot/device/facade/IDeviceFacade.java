package com.milesight.beaveriot.device.facade;

import com.milesight.beaveriot.base.enums.ComparisonOperator;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.device.dto.DeviceIdKeyDTO;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;

import java.util.List;
import java.util.Map;

public interface IDeviceFacade {
    List<Long> findDeviceIdsByGroupNameIn(List<String> deviceGroupNames);

    List<Long> fuzzySearchDeviceIdsByName(ComparisonOperator operator, String keyword);

    List<DeviceNameDTO> getDeviceNameByIntegrations(List<String> integrationIds);

    List<DeviceNameDTO> getDeviceNameByIds(List<Long> deviceIds);

    Map<String, Long> countByIntegrationIds(List<String> integrationIds);

    Long countByIntegrationId(String integrationId);

    Long countByTemplateInIgnoreTenant(List<String> templates);

    Device findById(Long id);

    List<DeviceIdKeyDTO> findIdAndKeyByIds(List<Long> deviceIds);

    List<DeviceIdKeyDTO> findIdAndKeyByKeys(List<String> deviceKeys);
}
