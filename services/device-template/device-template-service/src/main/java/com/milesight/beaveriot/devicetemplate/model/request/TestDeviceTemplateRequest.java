package com.milesight.beaveriot.devicetemplate.model.request;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/5/16 17:37
 **/
@Data
public class TestDeviceTemplateRequest {
    String integration;
    Object testData;
}
