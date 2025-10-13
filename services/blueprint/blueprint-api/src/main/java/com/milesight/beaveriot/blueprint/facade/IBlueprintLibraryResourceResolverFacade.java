package com.milesight.beaveriot.blueprint.facade;

import com.milesight.beaveriot.blueprint.model.BlueprintDeviceCodec;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceModel;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 17:23
 **/
public interface IBlueprintLibraryResourceResolverFacade {
    List<BlueprintDeviceVendor> getDeviceVendors();
    List<BlueprintDeviceVendor> getDeviceVendors(BlueprintLibrary blueprintLibrary);
    BlueprintDeviceVendor getDeviceVendor(String vendorId);
    BlueprintDeviceVendor getDeviceVendor(BlueprintLibrary blueprintLibrary, String vendorId);
    List<BlueprintDeviceModel> getDeviceModels(String vendorId);
    BlueprintDeviceModel getDeviceModel(String vendorId, String modelId);
    String getResourceContent(BlueprintLibrary blueprintLibrary, String vendorId, String relativePath);
    String getResourceContent(BlueprintLibrary blueprintLibrary, String resourcePath);
    String buildResourcePath(String basePath, String relativePath);
    String getDeviceTemplateContent(String vendorId, String modelId);
    String getDeviceTemplateContent(BlueprintLibrary blueprintLibrary, String vendorId, String modelId);
    BlueprintDeviceCodec getBlueprintDeviceCodec(BlueprintLibrary blueprintLibrary, String vendorId, String codecRelativePath, String codecId);
}
