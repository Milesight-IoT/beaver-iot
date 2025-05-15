package com.milesight.beaveriot.devicetemplate.facade;

import com.milesight.beaveriot.devicetemplate.dto.DeviceTemplateDTO;

import java.util.List;
import java.util.Map;

public interface IDeviceTemplateFacade {
    List<DeviceTemplateDTO> fuzzySearchDeviceTemplateByName(String name);

    List<DeviceTemplateDTO> getDeviceTemplateByIntegrations(List<String> integrationIds);

    List<DeviceTemplateDTO> getDeviceTemplateByIds(List<Long> deviceTemplateIds);

    List<DeviceTemplateDTO> getDeviceTemplateByKey(List<String> deviceTemplateKeys);

    DeviceTemplateDTO getDeviceTemplateByKey(String deviceTemplateKey);

    Map<String, Long> countByIntegrationIds(List<String> integrationIds);

    Long countByIntegrationId(String integrationId);
}
