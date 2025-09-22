package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.BlueprintDeviceModel;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 17:23
 **/
public interface BlueprintLibraryResourceResolverProvider {
    List<BlueprintDeviceVendor> getDeviceVendors();
    BlueprintDeviceVendor getDeviceVendor(String vendorId);
    List<BlueprintDeviceModel> getDeviceModels(String vendorId);
    BlueprintDeviceModel getDeviceModel(String vendorId, String modelId);
    String getDeviceTemplateContent(String vendorId, String modelId);
}
