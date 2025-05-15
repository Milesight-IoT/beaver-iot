package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.model.DeviceTemplateType;

/**
 * author: Luxb
 * create: 2025/5/15 10:06
 **/
public interface DeviceTemplateParserProvider {
    boolean validate(String deviceTemplateContent);
    String getDefaultDeviceTemplateContent();
    void discoveryDevice(Object data, String deviceTemplateContent);
}
