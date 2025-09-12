package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.BlueprintDevice;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 17:23
 **/
public interface BlueprintLibraryResourceResolverProvider {
    List<BlueprintDeviceVendor> getDeviceVendors();
    BlueprintDeviceVendor getDeviceVendor(String vendor);
    List<BlueprintDevice> getDevices(String vendor);
    BlueprintDevice getDevice(String vendor, String model);
    String getDeviceTemplateContent(String vendor, String model);
}
