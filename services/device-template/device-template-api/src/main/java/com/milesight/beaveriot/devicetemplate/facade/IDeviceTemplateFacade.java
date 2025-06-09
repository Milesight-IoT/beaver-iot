package com.milesight.beaveriot.devicetemplate.facade;

import com.milesight.beaveriot.devicetemplate.dto.DeviceTemplateDTO;

import java.util.List;

public interface IDeviceTemplateFacade {
    List<DeviceTemplateDTO> getDeviceTemplateByIds(List<Long> deviceTemplateIds);
}
