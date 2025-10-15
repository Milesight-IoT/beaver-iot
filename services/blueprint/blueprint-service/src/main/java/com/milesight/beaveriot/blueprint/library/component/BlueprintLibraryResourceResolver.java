package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.facade.IBlueprintLibraryResourceResolverFacade;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryErrorCode;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryResourceErrorCode;
import com.milesight.beaveriot.blueprint.library.model.BlueprintDeviceCodecs;
import com.milesight.beaveriot.blueprint.library.model.BlueprintDeviceModels;
import com.milesight.beaveriot.blueprint.library.model.BlueprintDeviceVendors;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryManifest;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryResource;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryResourceService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryService;
import com.milesight.beaveriot.blueprint.library.support.YamlConverter;
import com.milesight.beaveriot.blueprint.model.BlueprintDeviceCodec;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceModel;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
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
    public BlueprintDeviceVendor getDeviceVendor(String vendorId) {
        return getDeviceVendor(blueprintLibraryService.getCurrentBlueprintLibrary(), vendorId);
    }

    @Override
    public BlueprintDeviceVendor getDeviceVendor(BlueprintLibrary blueprintLibrary, String vendorId) {
        if (vendorId == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NULL).build();
        }

        List<BlueprintDeviceVendor> vendors = self().getDeviceVendors(blueprintLibrary);
        if (vendors == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDORS_NOT_FOUND).build();
        }

        for (BlueprintDeviceVendor vendor : vendors) {
            if (vendorId.equals(vendor.getId())) {
                return vendor;
            }
        }
        throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.getErrorCode(),
                BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.formatMessage(vendorId)).build();
    }

    @Override
    public List<BlueprintDeviceModel> getDeviceModels(String vendorId) {
        return self().getDeviceModels(blueprintLibraryService.getCurrentBlueprintLibrary(), vendorId);
    }

    @Override
    public BlueprintDeviceModel getDeviceModel(String vendorId, String modelId) {
        return getDeviceModel(blueprintLibraryService.getCurrentBlueprintLibrary(), vendorId, modelId);
    }

    public BlueprintDeviceModel getDeviceModel(BlueprintLibrary blueprintLibrary, String vendorId, String modelId) {
        if (vendorId == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NULL).build();
        }

        if (modelId == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NULL).build();
        }

        List<BlueprintDeviceModel> deviceModels = self().getDeviceModels(blueprintLibrary, vendorId);
        if (deviceModels == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODELS_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODELS_NOT_FOUND.formatMessage(vendorId)).build();
        }

        for (BlueprintDeviceModel deviceModel : deviceModels) {
            if (modelId.equals(deviceModel.getId())) {
                return deviceModel;
            }
        }
        throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NOT_FOUND.getErrorCode(),
                BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NOT_FOUND.formatMessage(vendorId, modelId)).build();
    }

    @Override
    public String getDeviceTemplateContent(String vendorId, String modelId) {
        return getDeviceTemplateContent(blueprintLibraryService.getCurrentBlueprintLibrary(), vendorId, modelId);
    }

    @Override
    public String getDeviceTemplateContent(BlueprintLibrary blueprintLibrary, String vendorId, String modelId) {
        BlueprintDeviceModel device = getDeviceModel(blueprintLibrary, vendorId, modelId);
        if (device == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NOT_FOUND.formatMessage(vendorId, modelId)).build();
        }

        if (StringUtils.isEmpty(device.getTemplate())) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_TEMPLATE_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_TEMPLATE_NOT_FOUND.formatMessage(vendorId, modelId)).build();
        }

        return getResourceContent(blueprintLibrary, vendorId, device.getTemplate());
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.type + ':' + #p0.url + '@' + #p0.branch + ':' + #p0.currentVersion", unless = "#result == null")
    public List<BlueprintDeviceVendor> getDeviceVendors(BlueprintLibrary blueprintLibrary) {
        if (blueprintLibrary == null) {
            return Collections.emptyList();
        }

        BlueprintLibraryManifest manifest = getManifest(blueprintLibrary);
        if (manifest == null) {
            return Collections.emptyList();
        }

        String vendorsContent = getResourceContent(blueprintLibrary, manifest.getDeviceVendorIndex());
        BlueprintDeviceVendors vendors = YamlConverter.from(vendorsContent, BlueprintDeviceVendors.class);
        if (vendors == null) {
            return Collections.emptyList();
        }
        return vendors.getVendors();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICE_VENDORS, key = "#p0.type + ':' + #p0.url + '@' + #p0.branch + ':' + #p0.currentVersion")
    public void evictCacheDeviceVendors(BlueprintLibrary blueprintLibrary) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintLibrary.getUrl(),
                blueprintLibrary.getBranch(),
                blueprintLibrary.getCurrentVersion());
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_DEVICE_MODELS, key = "#p0.type + ':' + #p0.url + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1", unless = "#result == null")
    public List<BlueprintDeviceModel> getDeviceModels(BlueprintLibrary blueprintLibrary, String vendorId) {
        BlueprintDeviceVendor vendorDef = getDeviceVendor(blueprintLibrary, vendorId);
        if (vendorDef == null) {
            return Collections.emptyList();
        }

        String resourcePath = buildResourcePath(vendorDef.getWorkDir(), vendorDef.getModelIndex());
        String devicesContent = getResourceContent(blueprintLibrary, resourcePath);
        BlueprintDeviceModels deviceModels = YamlConverter.from(devicesContent, BlueprintDeviceModels.class);
        if (deviceModels == null) {
            return Collections.emptyList();
        }
        return deviceModels.getModels();
    }

    @CacheEvict(cacheNames = Constants.CACHE_NAME_DEVICE_MODELS, key = "#p0.type + ':' + #p0.url + '@' + #p0.branch + ':' + #p0.currentVersion + ':' + #p1")
    public void evictCacheDeviceModels(BlueprintLibrary blueprintLibrary, String vendorId) {
        log.debug("Evict cache: {}, key: {}@{}:{}",
                Constants.CACHE_NAME_DEVICE_VENDORS,
                blueprintLibrary.getUrl(),
                blueprintLibrary.getBranch(),
                blueprintLibrary.getCurrentVersion());
    }

    public BlueprintDeviceCodecs getBlueprintDeviceCodecs(String vendorId, String codecRelativePath) {
        return getBlueprintDeviceCodecs(blueprintLibraryService.getCurrentBlueprintLibrary(), vendorId, codecRelativePath);
    }

    public BlueprintDeviceCodecs getBlueprintDeviceCodecs(BlueprintLibrary blueprintLibrary, String vendorId, String codecRelativePath) {
        String codecsContent = getResourceContent(blueprintLibrary, vendorId, codecRelativePath);
        return YamlConverter.from(codecsContent, BlueprintDeviceCodecs.class);
    }

    public BlueprintDeviceCodec getBlueprintDeviceCodec(String vendorId, String codecRelativePath, String codecId) {
        return getBlueprintDeviceCodec(blueprintLibraryService.getCurrentBlueprintLibrary(), vendorId, codecRelativePath, codecId);
    }

    @Override
    public BlueprintDeviceCodec getBlueprintDeviceCodec(BlueprintLibrary blueprintLibrary, String vendorId, String codecRelativePath, String codecId) {
        BlueprintDeviceCodecs blueprintDeviceCodecs = getBlueprintDeviceCodecs(blueprintLibrary, vendorId, codecRelativePath);
        if (blueprintDeviceCodecs == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND).build();
        }

        if (CollectionUtils.isEmpty(blueprintDeviceCodecs.getCodecs())) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND).build();
        }

        if (codecId == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND).build();
        }

        for (BlueprintDeviceCodec codec : blueprintDeviceCodecs.getCodecs()) {
            if (codecId.equals(codec.getId())) {
                return codec;
            }
        }
        throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND).build();
    }

    @Override
    public String getResourceContent(BlueprintLibrary blueprintLibrary, String vendorId, String relativePath) {
        String workDir = getWorkDirByVendor(blueprintLibrary, vendorId);
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

    public String getWorkDirByVendor(String vendorId) {
        return getWorkDirByVendor(blueprintLibraryService.getCurrentBlueprintLibrary(), vendorId);
    }

    private String getWorkDirByVendor(BlueprintLibrary blueprintLibrary, String vendorId) {
        BlueprintDeviceVendor vendor = getDeviceVendor(blueprintLibrary, vendorId);
        if (vendor == null) {
            throw ServiceException.with(BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.getErrorCode(),
                    BlueprintLibraryResourceErrorCode.BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND.formatMessage(vendorId)).build();
        }

        return vendor.getWorkDir();
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
            throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_NULL).build();
        }

        Assert.hasText(resourcePath, "'resourcePath' cannot be empty.");

        BlueprintLibraryResource blueprintLibraryResource = blueprintLibraryResourceService.getResource(blueprintLibrary.getId(), blueprintLibrary.getCurrentVersion(), resourcePath);
        if (blueprintLibraryResource == null) {
            return null;
        }

        return blueprintLibraryResource.getContent();
    }

    public static class Constants {
        public static final String CACHE_NAME_DEVICE_VENDORS = "blueprint-library:resource:device-vendors";
        public static final String CACHE_NAME_DEVICE_MODELS = "blueprint-library:resource:device-models";
        public static final String PATH_MANIFEST = "manifest.yaml";
    }
}
