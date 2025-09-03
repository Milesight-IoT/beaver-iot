package com.milesight.beaveriot.blueprint.component;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.model.*;
import com.milesight.beaveriot.blueprint.service.BlueprintRepositoryResourceService;
import com.milesight.beaveriot.blueprint.service.BlueprintRepositoryService;
import com.milesight.beaveriot.blueprint.support.YamlConverter;
import com.milesight.beaveriot.context.api.BlueprintRepositoryResourceProvider;
import com.milesight.beaveriot.context.integration.model.BlueprintDevice;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 9:55
 **/
@SuppressWarnings("unused")
@Slf4j
@Service
public class BlueprintRepositoryResourceResolver implements BlueprintRepositoryResourceProvider {
    private final BlueprintRepositoryService blueprintRepositoryService;
    private final BlueprintRepositoryResourceService blueprintRepositoryResourceService;

    public BlueprintRepositoryResourceResolver(BlueprintRepositoryService blueprintRepositoryService, BlueprintRepositoryResourceService blueprintRepositoryResourceService) {
        this.blueprintRepositoryService = blueprintRepositoryService;
        this.blueprintRepositoryResourceService = blueprintRepositoryResourceService;
    }

    @Override
    public List<BlueprintDeviceVendor> getDeviceVendors() {
        try {
            return self().getDeviceVendors(blueprintRepositoryService.getCurrentBlueprintRepository());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<BlueprintDevice> getDevices(String vendor) {
        try {
            return self().getDevices(blueprintRepositoryService.getCurrentBlueprintRepository(), vendor);
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
    public String getResourceContent(String vendor, String relativePath) {
        return getResourceContent(blueprintRepositoryService.getCurrentBlueprintRepository(), vendor, relativePath);
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':all'", unless = "#result == null")
    public List<BlueprintDeviceVendor> getDeviceVendors(BlueprintRepository blueprintRepository) {
        if (blueprintRepository == null) {
            return null;
        }

        BlueprintRepositoryManifest manifest = getManifest(blueprintRepository);
        if (manifest == null) {
            return null;
        }

        String vendorsContent = getResourceContent(blueprintRepository, manifest.getDeviceVendorIndex());
        BlueprintDeviceVendors vendors = YamlConverter.from(vendorsContent, BlueprintDeviceVendors.class);
        if (vendors == null) {
            return null;
        }
        return vendors.getVendors();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':all'")
    public void evictCacheDeviceVendors(BlueprintRepository blueprintRepository) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintRepository.getHome(),
                blueprintRepository.getBranch(),
                blueprintRepository.getCurrentVersion());
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICES, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1", unless = "#result == null")
    public List<BlueprintDevice> getDevices(BlueprintRepository blueprintRepository, String vendor) {
        BlueprintDeviceVendor vendorDef = getDeviceVendor(blueprintRepository, vendor);
        if (vendorDef == null) {
            return null;
        }

        String resourcePath = getResourcePath(vendorDef.getWorkDir(), vendorDef.getDeviceIndex());
        String devicesContent = getResourceContent(blueprintRepository, resourcePath);
        BlueprintDevices devices = YamlConverter.from(devicesContent, BlueprintDevices.class);
        if (devices == null) {
            return null;
        }
        return devices.getDevices();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICES, key = "#p0.home + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1")
    public void evictCacheDevices(BlueprintRepository blueprintRepository, String vendor) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintRepository.getHome(),
                blueprintRepository.getBranch(),
                blueprintRepository.getCurrentVersion());
    }

    public String getResourceContent(BlueprintRepository blueprintRepository, String vendor, String relativePath) {
        String workDir = getWorkDirByVendor(blueprintRepository, vendor);
        String resourcePath = getResourcePath(workDir, relativePath);
        return getResourceContent(blueprintRepository, resourcePath);
    }

    public BlueprintRepositoryResourceResolver self() {
        return SpringContext.getBean(BlueprintRepositoryResourceResolver.class);
    }

    private String getResourcePath(String basePath, String relativePath) {
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

    private BlueprintDeviceVendor getDeviceVendor(BlueprintRepository blueprintRepository, String vendor) {
        if (vendor == null) {
            return null;
        }

        try {
            List<BlueprintDeviceVendor> vendors = self().getDeviceVendors(blueprintRepository);
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

    private String getWorkDirByVendor(BlueprintRepository blueprintRepository, String vendor) {
        BlueprintDeviceVendor vendorDef = getDeviceVendor(blueprintRepository, vendor);
        if (vendorDef == null) {
            return null;
        }

        return vendorDef.getWorkDir();
    }

    private BlueprintRepositoryManifest getManifest(BlueprintRepository blueprintRepository) {
        String content = getResourceContent(blueprintRepository, Constants.PATH_MANIFEST);
        if (content == null) {
            return null;
        }
        return YamlConverter.from(content, BlueprintRepositoryManifest.class);
    }

    private String getResourceContent(BlueprintRepository blueprintRepository, String resourcePath) {
        if (blueprintRepository == null) {
            return null;
        }

        if (StringUtils.isEmpty(resourcePath)) {
            return null;
        }

        BlueprintRepositoryResource blueprintRepositoryResource = blueprintRepositoryResourceService.getResource(blueprintRepository.getId(), blueprintRepository.getCurrentVersion(), resourcePath);
        if (blueprintRepositoryResource == null) {
            return null;
        }

        return blueprintRepositoryResource.getContent();
    }

    public static class Constants {
        public static final String CACHE_NAME_DEVICE_VENDORS = "blueprint-repository:resource:device-vendors";
        public static final String CACHE_NAME_DEVICES = "blueprint-repository:resource:devices";
        public static final String PATH_MANIFEST = "manifest.yaml";
    }
}
