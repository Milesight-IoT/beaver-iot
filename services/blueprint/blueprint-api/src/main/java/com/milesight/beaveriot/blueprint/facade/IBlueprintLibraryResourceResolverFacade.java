package com.milesight.beaveriot.blueprint.facade;

import com.milesight.beaveriot.context.integration.model.BlueprintDevice;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 17:23
 **/
public interface IBlueprintLibraryResourceResolverFacade {
    List<BlueprintDeviceVendor> getDeviceVendors();
    BlueprintDeviceVendor getDeviceVendor(String vendor);
    List<BlueprintDevice> getDevices(String vendor);
    BlueprintDevice getDevice(String vendor, String model);
    String getResourceContent(String vendor, String relativePath);
    String getResourceContent(String resourcePath);
    String getResourcePath(String basePath, String relativePath);
    String getDeviceTemplateContent(String vendor, String model);
}
