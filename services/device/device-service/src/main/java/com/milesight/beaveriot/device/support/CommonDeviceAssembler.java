package com.milesight.beaveriot.device.support;

import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.device.location.constants.DeviceLocationConstants;
import com.milesight.beaveriot.device.status.constants.DeviceStatusConstants;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/8/19 10:12
 **/
@Component
public class CommonDeviceAssembler extends DeviceAssembler {
    protected CommonDeviceAssembler(EntityTemplateServiceProvider entityTemplateServiceProvider, EntityServiceProvider entityServiceProvider) {
        super(entityTemplateServiceProvider, entityServiceProvider);
    }

    @Override
    List<String> getCommonEntityKeys() {
        return List.of(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS, DeviceLocationConstants.IDENTIFIER_DEVICE_LOCATION);
    }
}
