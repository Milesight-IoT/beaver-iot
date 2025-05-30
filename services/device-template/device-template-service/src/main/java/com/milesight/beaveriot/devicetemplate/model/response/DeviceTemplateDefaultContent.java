package com.milesight.beaveriot.devicetemplate.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/5/16 17:35
 **/
@Data
public class DeviceTemplateDefaultContent {
    private String content;
    public static DeviceTemplateDefaultContent build(String content) {
        DeviceTemplateDefaultContent deviceTemplateDefaultContent = new DeviceTemplateDefaultContent();
        deviceTemplateDefaultContent.setContent(content);
        return deviceTemplateDefaultContent;
    }
}
