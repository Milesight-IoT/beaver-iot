package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.facade.IBlueprintLibraryResourceResolverFacade;
import com.milesight.beaveriot.blueprint.library.model.*;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryResourceService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryService;
import com.milesight.beaveriot.blueprint.library.support.YamlConverter;
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
public class BlueprintLibraryResourceResolverResolver implements IBlueprintLibraryResourceResolverFacade {
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibraryResourceService blueprintLibraryResourceService;

    public BlueprintLibraryResourceResolverResolver(BlueprintLibraryService blueprintLibraryService, com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryResourceService blueprintLibraryResourceService) {
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibraryResourceService = blueprintLibraryResourceService;
    }

    @Override
    public List<BlueprintDeviceVendor> getDeviceVendors() {
        try {
            return self().getDeviceVendors(blueprintLibraryService.getCurrentBlueprintLibrary());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BlueprintDeviceVendor getDeviceVendor(String vendor) {
        if (vendor == null) {
            return null;
        }

        List<BlueprintDeviceVendor> deviceVendors = self().getDeviceVendors();
        if (deviceVendors == null) {
            return null;
        }

        for (BlueprintDeviceVendor eachVendor : deviceVendors) {
            if (vendor.equals(eachVendor.getId())) {
                return eachVendor;
            }
        }
        return null;
    }

    @Override
    public List<BlueprintDevice> getDevices(String vendor) {
        try {
            return self().getDevices(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BlueprintDevice getDevice(String vendor, String model) {
        if (vendor == null || model == null) {
            return null;
        }

        List<BlueprintDevice> devices = self().getDevices(vendor);
        if (devices == null) {
            return null;
        }

        for (BlueprintDevice eachDevice : devices) {
            if (model.equals(eachDevice.getId())) {
                return eachDevice;
            }
        }
        return null;
    }

    @Override
    public String getDeviceTemplateContent(String vendor, String model) {
        BlueprintDevice device = getDevice(vendor, model);
        if (device == null) {
            return null;
        }

        if (StringUtils.isEmpty(device.getDevice())) {
            return null;
        }

        return getResourceContent(vendor, device.getDevice());
    }

    @Override
    public String getResourceContent(String vendor, String relativePath) {
        return getResourceContent(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor, relativePath);
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':all'", unless = "#result == null")
    public List<BlueprintDeviceVendor> getDeviceVendors(BlueprintLibrary blueprintLibrary) {
        if (blueprintLibrary == null) {
            return null;
        }

        BlueprintLibraryManifest manifest = getManifest(blueprintLibrary);
        if (manifest == null) {
            return null;
        }

        String vendorsContent = getResourceContent(blueprintLibrary, manifest.getDeviceVendorIndex());
        BlueprintDeviceVendors vendors = YamlConverter.from(vendorsContent, BlueprintDeviceVendors.class);
        if (vendors == null) {
            return null;
        }
        return vendors.getVendors();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':all'")
    public void evictCacheDeviceVendors(BlueprintLibrary blueprintLibrary) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintLibrary.getHome(),
                blueprintLibrary.getBranch(),
                blueprintLibrary.getCurrentVersion());
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICES, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1", unless = "#result == null")
    public List<BlueprintDevice> getDevices(BlueprintLibrary blueprintLibrary, String vendor) {
        BlueprintDeviceVendor vendorDef = getDeviceVendor(blueprintLibrary, vendor);
        if (vendorDef == null) {
            return null;
        }

        String resourcePath = getResourcePath(vendorDef.getWorkDir(), vendorDef.getDeviceIndex());
        String devicesContent = getResourceContent(blueprintLibrary, resourcePath);
        BlueprintDevices devices = YamlConverter.from(devicesContent, BlueprintDevices.class);
        if (devices == null) {
            return null;
        }
        return devices.getDevices();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICES, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1")
    public void evictCacheDevices(BlueprintLibrary blueprintLibrary, String vendor) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintLibrary.getHome(),
                blueprintLibrary.getBranch(),
                blueprintLibrary.getCurrentVersion());
    }

    public BlueprintDeviceCodecs getBlueprintDeviceCodecs(String vendor, String codecRelativePath) {
        String codecsContent = getResourceContent(vendor, codecRelativePath);
        return YamlConverter.from(codecsContent, BlueprintDeviceCodecs.class);
    }

    public BlueprintDeviceCodec getBlueprintDeviceCodec(String vendor, String codecRelativePath, String codecId) {
        BlueprintDeviceCodecs blueprintDeviceCodecs = getBlueprintDeviceCodecs(vendor, codecRelativePath);
        if (blueprintDeviceCodecs == null) {
            return null;
        }

        if (CollectionUtils.isEmpty(blueprintDeviceCodecs.getCodecs())) {
            return null;
        }

        if (codecId == null) {
            return null;
        }

        for (BlueprintDeviceCodec eachCodec : blueprintDeviceCodecs.getCodecs()) {
            if (codecId.equals(eachCodec.getId())) {
                return eachCodec;
            }
        }
        return null;
    }

    public String getResourceContent(BlueprintLibrary blueprintLibrary, String vendor, String relativePath) {
        String workDir = getWorkDirByVendor(blueprintLibrary, vendor);
        String resourcePath = getResourcePath(workDir, relativePath);
        return getResourceContent(blueprintLibrary, resourcePath);
    }

    public BlueprintLibraryResourceResolverResolver self() {
        return SpringContext.getBean(BlueprintLibraryResourceResolverResolver.class);
    }

    @Override
    public String getResourcePath(String basePath, String relativePath) {
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

    private BlueprintDeviceVendor getDeviceVendor(BlueprintLibrary blueprintLibrary, String vendor) {
        if (vendor == null) {
            return null;
        }

        try {
            List<BlueprintDeviceVendor> vendors = self().getDeviceVendors(blueprintLibrary);
            if (vendors == null) {
                return null;
            }

            for (BlueprintDeviceVendor eachVendor : vendors) {
                if (vendor.equals(eachVendor.getId())) {
                    return eachVendor;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getWorkDirByVendor(String vendor) {
        return getWorkDirByVendor(blueprintLibraryService.getCurrentBlueprintLibrary(), vendor);
    }

    private String getWorkDirByVendor(BlueprintLibrary blueprintLibrary, String vendor) {
        BlueprintDeviceVendor vendorDef = getDeviceVendor(blueprintLibrary, vendor);
        if (vendorDef == null) {
            return null;
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

    @Override
    public String getResourceContent(String resourcePath) {
        return getResourceContent(blueprintLibraryService.getCurrentBlueprintLibrary(), resourcePath);
    }

    private String getResourceContent(BlueprintLibrary blueprintLibrary, String resourcePath) {
        if (blueprintLibrary == null) {
            return null;
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
