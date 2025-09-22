package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.context.api.BlueprintLibraryResourceResolverProvider;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceModel;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/12 14:23
 **/
@Service
public class BlueprintLibraryResourceResolverProviderImpl implements BlueprintLibraryResourceResolverProvider {
    private final BlueprintLibraryResourceResolver blueprintLibraryResourceResolver;

    public BlueprintLibraryResourceResolverProviderImpl(BlueprintLibraryResourceResolver blueprintLibraryResourceResolver) {
        this.blueprintLibraryResourceResolver = blueprintLibraryResourceResolver;
    }

    @Override
    public List<BlueprintDeviceVendor> getDeviceVendors() {
        return blueprintLibraryResourceResolver.getDeviceVendors();
    }

    @Override
    public BlueprintDeviceVendor getDeviceVendor(String vendorId) {
        return blueprintLibraryResourceResolver.getDeviceVendor(vendorId);
    }

    @Override
    public List<BlueprintDeviceModel> getDeviceModels(String vendorId) {
        return blueprintLibraryResourceResolver.getDeviceModels(vendorId);
    }

    @Override
    public BlueprintDeviceModel getDeviceModel(String vendorId, String modelId) {
        return blueprintLibraryResourceResolver.getDeviceModel(vendorId, modelId);
    }

    @Override
    public String getDeviceTemplateContent(String vendorId, String modelId) {
        return blueprintLibraryResourceResolver.getDeviceTemplateContent(vendorId, modelId);
    }
}
