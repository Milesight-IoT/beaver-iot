package com.milesight.beaveriot.devicetemplate.facade;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;

public interface IDeviceTemplateParserFacade {
    boolean validate(String deviceTemplateContent);
    String defaultContent();
    DeviceTemplateModel parse(String deviceTemplateContent);
    DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String jsonData);
    DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload);
    Device createDevice(String integration, Long deviceTemplateId, String deviceId, String deviceName);
}
