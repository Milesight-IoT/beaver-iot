package com.milesight.beaveriot.devicetemplate.facade;

import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;

public interface IDeviceTemplateParserFacade {
    boolean validate(String deviceTemplateContent);
    DeviceTemplateDiscoverResponse discover(String integration, Object data, String deviceTemplateKey, String deviceTemplateContent);
}
