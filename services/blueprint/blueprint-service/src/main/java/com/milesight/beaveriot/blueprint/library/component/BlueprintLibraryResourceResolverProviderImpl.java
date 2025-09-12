package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.context.api.BlueprintLibraryResourceResolverProvider;
import com.milesight.beaveriot.context.integration.model.BlueprintDevice;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/12 14:23
 **/
@Service
public class BlueprintLibraryResourceResolverProviderImpl implements BlueprintLibraryResourceResolverProvider {
    private final BlueprintLibraryResourceResolverResolver blueprintLibraryResourceResolver;

    public BlueprintLibraryResourceResolverProviderImpl(BlueprintLibraryResourceResolverResolver blueprintLibraryResourceResolver) {
        this.blueprintLibraryResourceResolver = blueprintLibraryResourceResolver;
    }

    @Override
    public List<BlueprintDeviceVendor> getDeviceVendors() {
        return blueprintLibraryResourceResolver.getDeviceVendors();
    }

    @Override
    public BlueprintDeviceVendor getDeviceVendor(String vendor) {
        return blueprintLibraryResourceResolver.getDeviceVendor(vendor);
    }

    @Override
    public List<BlueprintDevice> getDevices(String vendor) {
        return blueprintLibraryResourceResolver.getDevices(vendor);
    }

    @Override
    public BlueprintDevice getDevice(String vendor, String model) {
        return blueprintLibraryResourceResolver.getDevice(vendor, model);
    }

    @Override
    public String getDeviceTemplateContent(String vendor, String model) {
        return blueprintLibraryResourceResolver.getDeviceTemplateContent(vendor, model);
    }
}
