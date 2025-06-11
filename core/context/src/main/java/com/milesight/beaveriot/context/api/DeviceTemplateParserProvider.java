package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;

/**
 * author: Luxb
 * create: 2025/5/15 10:06
 **/
public interface DeviceTemplateParserProvider {
    boolean validate(String deviceTemplateContent);
    String defaultContent();
    DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String jsonData);
    DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload);
}
