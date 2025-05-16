package com.milesight.beaveriot.context.api;

/**
 * author: Luxb
 * create: 2025/5/15 10:06
 **/
public interface DeviceTemplateParserProvider {
    boolean validate(String deviceTemplateContent);
    String getDefaultDeviceTemplateContent();
    void discover(String integration, Object data, Long deviceTemplateId, String deviceTemplateContent);
}
