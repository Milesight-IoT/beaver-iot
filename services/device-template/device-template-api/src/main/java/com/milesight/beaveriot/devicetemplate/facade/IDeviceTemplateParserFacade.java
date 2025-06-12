package com.milesight.beaveriot.devicetemplate.facade;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;

public interface IDeviceTemplateParserFacade {
    boolean validate(String deviceTemplateContent);
    String defaultContent();
    DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String jsonData);
    DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload);
}
