package com.milesight.beaveriot.blueprint.facade;

import com.milesight.beaveriot.blueprint.model.BlueprintDeviceCodec;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.integration.model.BlueprintDevice;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 17:23
 **/
public interface IBlueprintLibraryResourceResolverFacade {
    List<BlueprintDeviceVendor> getDeviceVendors();
    List<BlueprintDeviceVendor> getDeviceVendors(BlueprintLibrary blueprintLibrary);
    BlueprintDeviceVendor getDeviceVendor(String vendor);
    BlueprintDeviceVendor getDeviceVendor(BlueprintLibrary blueprintLibrary, String vendor);
    List<BlueprintDevice> getDevices(String vendor);
    BlueprintDevice getDevice(String vendor, String model);
    String getResourceContent(String vendor, String relativePath);
    String getResourceContent(BlueprintLibrary blueprintLibrary, String resourcePath);
    String buildResourcePath(String basePath, String relativePath);
    String getDeviceTemplateContent(String vendor, String model);
    String getDeviceTemplateContent(BlueprintLibrary blueprintLibrary, String vendor, String model);
    BlueprintDeviceCodec getBlueprintDeviceCodec(BlueprintLibrary blueprintLibrary, String vendor, String codecRelativePath, String codecId);
}
