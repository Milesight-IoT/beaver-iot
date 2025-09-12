package com.milesight.beaveriot.context.model.response;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/9 14:58
 **/
@Data
public class DeviceTemplateInputResult {
    private Device device;
    private ExchangePayload payload;
    private boolean isDeviceAutoSaved = false;
}
