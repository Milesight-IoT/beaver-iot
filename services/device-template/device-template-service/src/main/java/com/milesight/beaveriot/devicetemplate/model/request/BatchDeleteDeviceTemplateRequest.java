package com.milesight.beaveriot.devicetemplate.model.request;

import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteDeviceTemplateRequest {
    private List<String> idList;
}
