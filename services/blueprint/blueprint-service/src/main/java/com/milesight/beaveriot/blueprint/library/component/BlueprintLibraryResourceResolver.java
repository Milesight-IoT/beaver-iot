package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.facade.IBlueprintLibraryResourceResolverFacade;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryErrorCode;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryResourceErrorCode;
import com.milesight.beaveriot.blueprint.library.model.*;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryResourceService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryService;
import com.milesight.beaveriot.blueprint.library.support.YamlConverter;
import com.milesight.beaveriot.blueprint.model.BlueprintDeviceCodec;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.integration.model.BlueprintDevice;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 9:55
 **/
@SuppressWarnings("unused")
@Slf4j
@Service
public class BlueprintLibraryResourceResolver implements IBlueprintLibraryResourceResolverFacade {
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibraryResourceService blueprintLibraryResourceService;

    public BlueprintLibraryResourceResolver(BlueprintLibraryService blueprintLibraryService, BlueprintLibraryResourceService blueprintLibraryResourceService) {
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibraryResourceService = blueprintLibraryResourceService;
    }

    @Override
    public List<BlueprintDeviceVendor> getDeviceVendors() {
        return self().getDeviceVendors(blueprintLibraryService.getCurrentBlueprintLibrary());
    }

    @Override
    public BlueprintDeviceVendor getDeviceVendor(String vendor) {
        return getDeviceVendor(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor);
    }

    @Override
    public BlueprintDeviceVendor getDeviceVendor(BlueprintLibrary blueprintLibrary, String vendor) {
        if (vendor == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NULL.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NULL.getErrorMessage()).build();
        }

        List<BlueprintDeviceVendor> vendors = self().getDeviceVendors(blueprintLibrary);
        if (vendors == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDORS_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDORS_NOT_FOUND.getErrorMessage()).build();
        }

        for (BlueprintDeviceVendor eachVendor : vendors) {
            if (vendor.equals(eachVendor.getId())) {
                return eachVendor;
            }
        }
        throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.getErrorCode(),
                BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.formatMessage(vendor)).build();
    }

    @Override
    public List<BlueprintDevice> getDevices(String vendor) {
        return self().getDevices(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor);
    }

    @Override
    public BlueprintDevice getDevice(String vendor, String model) {
        return getDevice(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor, model);
    }

    public BlueprintDevice getDevice(BlueprintLibrary blueprintLibrary, String vendor, String model) {
        if (vendor == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NULL.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NULL.getErrorMessage()).build();
        }

        if (model == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NULL.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NULL.getErrorMessage()).build();
        }

        List<BlueprintDevice> devices = self().getDevices(blueprintLibrary, vendor);
        if (devices == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICES_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICES_NOT_FOUND.formatMessage(vendor)).build();
        }

        for (BlueprintDevice eachDevice : devices) {
            if (model.equals(eachDevice.getId())) {
                return eachDevice;
            }
        }
        throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_NOT_FOUND.getErrorCode(),
                BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_NOT_FOUND.formatMessage(vendor, model)).build();
    }

    @Override
    public String getDeviceTemplateContent(String vendor, String model) {
        return getDeviceTemplateContent(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor, model);
    }

    @Override
    public String getDeviceTemplateContent(BlueprintLibrary blueprintLibrary, String vendor, String model) {
        BlueprintDevice device = getDevice(blueprintLibrary, vendor, model);
        if (device == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_NOT_FOUND.formatMessage(vendor, model)).build();
        }

        if (StringUtils.isEmpty(device.getDevice())) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_TEMPLATE_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_TEMPLATE_NOT_FOUND.formatMessage(vendor, model)).build();
        }

        return getResourceContent(blueprintLibrary, vendor, device.getDevice());
    }

    @Override
    public String getResourceContent(String vendor, String relativePath) {
        return getResourceContent(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor, relativePath);
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.type + ':' + #p0.home + '@' + #p0.branch + ':' + #p0.currentVersion", unless = "#result == null")
    public List<BlueprintDeviceVendor> getDeviceVendors(BlueprintLibrary blueprintLibrary) {
        if (blueprintLibrary == null) {
            throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_NULL.getErrorCode(),
                    BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_NULL.getErrorMessage()).build();
        }

        BlueprintLibraryManifest manifest = getManifest(blueprintLibrary);
        if (manifest == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_MANIFEST_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_MANIFEST_NOT_FOUND.getErrorMessage()).build();
        }

        String vendorsContent = getResourceContent(blueprintLibrary, manifest.getDeviceVendorIndex());
        BlueprintDeviceVendors vendors = YamlConverter.from(vendorsContent, BlueprintDeviceVendors.class);
        if (vendors == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDORS_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDORS_NOT_FOUND.getErrorMessage()).build();
        }
        return vendors.getVendors();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.type + ':' + #p0.home + '@' + #p0.branch + ':' + #p0.currentVersion")
    public void evictCacheDeviceVendors(BlueprintLibrary blueprintLibrary) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintLibrary.getHome(),
                blueprintLibrary.getBranch(),
                blueprintLibrary.getCurrentVersion());
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICES, key = "#p0.type + ':' + #p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1", unless = "#result == null")
    public List<BlueprintDevice> getDevices(BlueprintLibrary blueprintLibrary, String vendor) {
        BlueprintDeviceVendor vendorDef = getDeviceVendor(blueprintLibrary, vendor);
        if (vendorDef == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.formatMessage(vendor)).build();
        }

        String resourcePath = buildResourcePath(vendorDef.getWorkDir(), vendorDef.getDeviceIndex());
        String devicesContent = getResourceContent(blueprintLibrary, resourcePath);
        BlueprintDevices devices = YamlConverter.from(devicesContent, BlueprintDevices.class);
        if (devices == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICES_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICES_NOT_FOUND.formatMessage(vendor)).build();
        }
        return devices.getDevices();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICES, key = "#p0.type + ':' + #p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1")
    public void evictCacheDevices(BlueprintLibrary blueprintLibrary, String vendor) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintLibrary.getHome(),
                blueprintLibrary.getBranch(),
                blueprintLibrary.getCurrentVersion());
    }

    public BlueprintDeviceCodecs getBlueprintDeviceCodecs(String vendor, String codecRelativePath) {
        return getBlueprintDeviceCodecs(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor, codecRelativePath);
    }

    public BlueprintDeviceCodecs getBlueprintDeviceCodecs(BlueprintLibrary blueprintLibrary, String vendor, String codecRelativePath) {
        String codecsContent = getResourceContent(blueprintLibrary, vendor, codecRelativePath);
        return YamlConverter.from(codecsContent, BlueprintDeviceCodecs.class);
    }

    public BlueprintDeviceCodec getBlueprintDeviceCodec(String vendor, String codecRelativePath, String codecId) {
        return getBlueprintDeviceCodec(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor, codecRelativePath, codecId);
    }

    @Override
    public BlueprintDeviceCodec getBlueprintDeviceCodec(BlueprintLibrary blueprintLibrary, String vendor, String codecRelativePath, String codecId) {
        BlueprintDeviceCodecs blueprintDeviceCodecs = getBlueprintDeviceCodecs(blueprintLibrary, vendor, codecRelativePath);
        if (blueprintDeviceCodecs == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorMessage()).build();
        }

        if (CollectionUtils.isEmpty(blueprintDeviceCodecs.getCodecs())) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorMessage()).build();
        }

        if (codecId == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorMessage()).build();
        }

        for (BlueprintDeviceCodec eachCodec : blueprintDeviceCodecs.getCodecs()) {
            if (codecId.equals(eachCodec.getId())) {
                return eachCodec;
            }
        }
        throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorCode(),
                BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND.getErrorMessage()).build();
    }

    public String getResourceContent(BlueprintLibrary blueprintLibrary, String vendor, String relativePath) {
        String workDir = getWorkDirByVendor(blueprintLibrary, vendor);
        String resourcePath = buildResourcePath(workDir, relativePath);
        return getResourceContent(blueprintLibrary, resourcePath);
    }

    public BlueprintLibraryResourceResolver self() {
        return SpringContext.getBean(BlueprintLibraryResourceResolver.class);
    }

    @Override
    public String buildResourcePath(String basePath, String relativePath) {
        if (StringUtils.isEmpty(basePath) || StringUtils.isEmpty(relativePath)) {
            return null;
        }

        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return basePath + "/" + relativePath;
    }

    public String getWorkDirByVendor(String vendor) {
        return getWorkDirByVendor(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor);
    }

    private String getWorkDirByVendor(BlueprintLibrary blueprintLibrary, String vendor) {
        BlueprintDeviceVendor vendorDef = getDeviceVendor(blueprintLibrary, vendor);
        if (vendorDef == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.formatMessage(vendor)).build();
        }

        return vendorDef.getWorkDir();
    }

    private BlueprintLibraryManifest getManifest(BlueprintLibrary blueprintLibrary) {
        String content = getResourceContent(blueprintLibrary, Constants.PATH_MANIFEST);
        if (content == null) {
            return null;
        }
        return YamlConverter.from(content, BlueprintLibraryManifest.class);
    }

    public String getResourceContent(String resourcePath) {
        return getResourceContent(blueprintLibraryService.getCurrentBlueprintLibrary(), resourcePath);
    }

    @Override
    public String getResourceContent(BlueprintLibrary blueprintLibrary, String resourcePath) {
        if (blueprintLibrary == null) {
            throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_NULL.getErrorCode(),
                    BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_NULL.getErrorMessage()).build();
        }

        if (StringUtils.isEmpty(resourcePath)) {
            return null;
        }

        BlueprintLibraryResource blueprintLibraryResource = blueprintLibraryResourceService.getResource(blueprintLibrary.getId(), blueprintLibrary.getCurrentVersion(), resourcePath);
        if (blueprintLibraryResource == null) {
            return null;
        }

        return blueprintLibraryResource.getContent();
    }

    public static class Constants {
        public static final String CACHE_NAME_DEVICE_VENDORS = "blueprint-library:resource:device-vendors";
        public static final String CACHE_NAME_DEVICES = "blueprint-library:resource:devices";
        public static final String PATH_MANIFEST = "manifest.yaml";
    }
}
