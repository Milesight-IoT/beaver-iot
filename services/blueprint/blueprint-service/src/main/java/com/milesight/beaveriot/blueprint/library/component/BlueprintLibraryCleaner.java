package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryVersion;
import com.milesight.beaveriot.blueprint.library.service.*;
import com.milesight.beaveriot.context.enums.ResourceRefType;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateFacade;
import com.milesight.beaveriot.context.model.ResourceRefDTO;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import com.milesight.beaveriot.user.facade.ITenantFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/30 14:14
 **/
@Slf4j
@Service
public class BlueprintLibraryCleaner {
    private final IDeviceTemplateFacade deviceTemplateFacade;
    private final BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService;
    private final BlueprintLibraryResourceService blueprintLibraryResourceService;
    private final BlueprintLibraryVersionService blueprintLibraryVersionService;
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;
    private final ResourceManagerFacade resourceManagerFacade;
    private final ITenantFacade tenantFacade;
    private final BlueprintLibrarySyncer blueprintLibrarySyncer;

    public BlueprintLibraryCleaner(IDeviceTemplateFacade deviceTemplateFacade, BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService, BlueprintLibraryResourceService blueprintLibraryResourceService, BlueprintLibraryVersionService blueprintLibraryVersionService, BlueprintLibraryService blueprintLibraryService, BlueprintLibraryAddressService blueprintLibraryAddressService, ResourceManagerFacade resourceManagerFacade, ITenantFacade tenantFacade, BlueprintLibrarySyncer blueprintLibrarySyncer) {
        this.deviceTemplateFacade = deviceTemplateFacade;
        this.blueprintLibrarySubscriptionService = blueprintLibrarySubscriptionService;
        this.blueprintLibraryResourceService = blueprintLibraryResourceService;
        this.blueprintLibraryVersionService = blueprintLibraryVersionService;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.resourceManagerFacade = resourceManagerFacade;
        this.tenantFacade = tenantFacade;
        this.blueprintLibrarySyncer = blueprintLibrarySyncer;
    }

    @DistributedLock(name = "blueprint-library-clean-#{#p0.libraryId}:#{#p0.libraryVersion}", waitForLock = "10s", scope = LockScope.GLOBAL, throwOnLockFailure = false)
    public void clean(BlueprintLibraryVersion blueprintLibraryVersion) {
        long start = System.currentTimeMillis();
        // Double check
        BlueprintLibraryVersion existBlueprintLibraryVersion = blueprintLibraryVersionService.findByLibraryIdAndLibraryVersion(blueprintLibraryVersion.getLibraryId(), blueprintLibraryVersion.getLibraryVersion());
        if (existBlueprintLibraryVersion == null) {
            return;
        }

        log.debug("Start cleaning blueprint library version {}", blueprintLibraryVersion.getKey());

        try {
            Long libraryId = blueprintLibraryVersion.getLibraryId();
            String libraryVersion = blueprintLibraryVersion.getLibraryVersion();
            List<DeviceTemplate> deviceTemplates = deviceTemplateFacade.findByBlueprintLibraryIgnoreTenant(libraryId, libraryVersion);
            if (!CollectionUtils.isEmpty(deviceTemplates)) {
                List<Long> toDeleteIds = deviceTemplates.stream().map(DeviceTemplate::getId).toList();
                deviceTemplateFacade.deleteDeviceTemplateByIdInIgnoreTenant(toDeleteIds);
            }

            blueprintLibrarySubscriptionService.deleteByLibraryIdAndLibraryVersionIgnoreTenant(libraryId, libraryVersion);
            blueprintLibraryResourceService.deleteAllByLibraryIdAndLibraryVersion(libraryId, libraryVersion);
            blueprintLibraryVersionService.deleteByLibraryIdAndLibraryVersion(libraryId, libraryVersion);

            BlueprintLibrary blueprintLibrary = blueprintLibraryService.findById(libraryId);
            long existVersionCount = blueprintLibraryVersionService.countByLibraryId(libraryId);
            if (existVersionCount == 0) {
                self().deleteBlueprintLibrary(blueprintLibrary);
            }

            BlueprintLibrary oldBlueprintLibrary = BlueprintLibrary.clone(blueprintLibrary);
            oldBlueprintLibrary.setCurrentVersion(libraryVersion);
            tenantFacade.runWithAllTenants(() -> blueprintLibrarySyncer.evictCaches(oldBlueprintLibrary));

            log.debug("Fishing cleaning blueprint library version: {}, time: {} ms", blueprintLibraryVersion.getKey(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Clean blueprint library version {} failed", blueprintLibraryVersion.getKey(), e);
            throw e;
        }
    }

    public BlueprintLibraryCleaner self() {
        return SpringContext.getBean(BlueprintLibraryCleaner.class);
    }

    @DistributedLock(name = "blueprint-library-delete-#{#p0.id}", waitForLock = "5s", scope = LockScope.GLOBAL, throwOnLockFailure = false)
    public void deleteBlueprintLibrary(BlueprintLibrary blueprintLibrary) {
        // Double check
        BlueprintLibrary existBlueprintLibrary = blueprintLibraryService.findById(blueprintLibrary.getId());
        if (existBlueprintLibrary == null) {
            return;
        }

        BlueprintLibraryAddress blueprintLibraryAddress = blueprintLibraryAddressService.convertLibraryToAddress(blueprintLibrary);
        blueprintLibraryService.deleteById(blueprintLibrary.getId());
        blueprintLibraryService.evictCacheBlueprintLibrary(blueprintLibrary.getType().name(), blueprintLibrary.getUrl(), blueprintLibrary.getBranch());
        tenantFacade.runWithAllTenants(() -> tryUnlinkResource(blueprintLibraryAddress));
    }

    private void tryUnlinkResource(BlueprintLibraryAddress blueprintLibraryAddress) {
        if (blueprintLibraryAddress.getSourceType() == BlueprintLibrarySourceType.UPLOAD) {
            try {
                ResourceRefDTO resourceRefDTO = new ResourceRefDTO(blueprintLibraryAddress.getKey(), ResourceRefType.BLUEPRINT_LIBRARY_ADDRESS.name());
                resourceManagerFacade.unlinkRef(resourceRefDTO);
            } catch (Exception e) {
                log.warn("Try unlink url {} to resource failed.", blueprintLibraryAddress.getUrl());
            }
        }
    }
}
