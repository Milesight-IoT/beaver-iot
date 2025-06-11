package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;

/**
 * author: Luxb
 * create: 2025/5/15 10:06
 **/
public interface DeviceTemplateParserProvider {
    boolean validate(String deviceTemplateContent);
    String defaultContent();
    DeviceTemplateDiscoverResponse discover(String integration, String jsonData, Long deviceTemplateId, String deviceTemplateContent);
    String output(String deviceKey, ExchangePayload payload);
}
