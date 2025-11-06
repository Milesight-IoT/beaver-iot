package com.milesight.beaveriot.blueprint.library.controller;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.blueprint.library.component.BlueprintLibrarySyncer;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryErrorCode;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibrarySubscription;
import com.milesight.beaveriot.blueprint.library.model.request.SaveBlueprintLibrarySettingRequest;
import com.milesight.beaveriot.blueprint.library.model.response.QueryBlueprintLibrarySettingResponse;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryAddressService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibrarySubscriptionService;
import com.milesight.beaveriot.context.enums.ResourceRefType;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.model.BlueprintLibrarySyncStatus;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.context.model.ResourceRefDTO;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * author: Luxb
 * create: 2025/9/17 15:05
 **/
@Slf4j
@RestController
@RequestMapping("/blueprint-library-setting")
public class BlueprintLibrarySettingController {
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;
    private final BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService;
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibrarySyncer blueprintLibrarySyncer;
    private final ResourceManagerFacade resourceManagerFacade;

    public BlueprintLibrarySettingController(BlueprintLibraryAddressService blueprintLibraryAddressService, BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService, BlueprintLibraryService blueprintLibraryService, BlueprintLibrarySyncer blueprintLibrarySyncer, ResourceManagerFacade resourceManagerFacade) {
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.blueprintLibrarySubscriptionService = blueprintLibrarySubscriptionService;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibrarySyncer = blueprintLibrarySyncer;
        this.resourceManagerFacade = resourceManagerFacade;
    }

    @OperationPermission(codes = {OperationPermissionCode.CREDENTIALS_VIEW, OperationPermissionCode.CREDENTIALS_EDIT})
    @GetMapping("")
    public ResponseBody<QueryBlueprintLibrarySettingResponse> getBlueprintLibrarySetting() {
        QueryBlueprintLibrarySettingResponse response = new QueryBlueprintLibrarySettingResponse();

        BlueprintLibraryAddress activeBlueprintLibraryAddress = blueprintLibraryAddressService.findByActiveTrue();
        if (activeBlueprintLibraryAddress == null) {
            activeBlueprintLibraryAddress = blueprintLibraryAddressService.getDefaultBlueprintLibraryAddress();
        } else {
            if (activeBlueprintLibraryAddress.getSourceType() == BlueprintLibrarySourceType.DEFAULT && !blueprintLibraryAddressService.isDefaultBlueprintLibraryAddress(activeBlueprintLibraryAddress)) {
                activeBlueprintLibraryAddress = blueprintLibraryAddressService.getDefaultBlueprintLibraryAddress();
            }
        }

        BlueprintLibrary activeBlueprintLibrary = blueprintLibraryService.getBlueprintLibrary(activeBlueprintLibraryAddress.getType().name(), activeBlueprintLibraryAddress.getUrl(), activeBlueprintLibraryAddress.getBranch());
        if (activeBlueprintLibrary != null) {
            if (activeBlueprintLibraryAddress.getSourceType() == BlueprintLibrarySourceType.UPLOAD) {
                response.setCurrentSourceType(BlueprintLibrarySourceType.UPLOAD.name());
                response.setFileName(getZipFileFromUrl(activeBlueprintLibrary.getUrl()));
            } else {
                response.setCurrentSourceType(BlueprintLibrarySourceType.DEFAULT.name());
            }
            response.setVersion(activeBlueprintLibrary.getCurrentVersion());
            response.setUpdateTime(activeBlueprintLibrary.getSyncedAt());
            boolean syncedSuccess = activeBlueprintLibrary.getSyncStatus() == BlueprintLibrarySyncStatus.SYNCED;
            response.setSyncedSuccess(syncedSuccess);
            response.setType(activeBlueprintLibrary.getType().name());
            response.setUrl(activeBlueprintLibrary.getUrl());
        }

        return ResponseBuilder.success(response);
    }

    private String getZipFileFromUrl(String url) {
        return resourceManagerFacade.getResourceNameByUrl(url);
    }

    @OperationPermission(codes = OperationPermissionCode.CREDENTIALS_EDIT)
    @PostMapping("")
    public ResponseBody<Void> saveBlueprintLibrarySetting(@RequestBody SaveBlueprintLibrarySettingRequest request) throws Exception {
        String sourceType = request.getSourceType();
        if (!validateSourceType(sourceType)) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_SOURCE_TYPE_NOT_SUPPORTED).build();
        }

        String type = request.getType();
        String url = request.getUrl();
        String branch = request.getBranch();

        // Step 1. Get blueprint library address
        BlueprintLibraryAddress blueprintLibraryAddress;
        if (BlueprintLibrarySourceType.DEFAULT.name().equals(sourceType)) {
            blueprintLibraryAddress = blueprintLibraryAddressService.getDefaultBlueprintLibraryAddress();
        } else {
            blueprintLibraryAddress = BlueprintLibraryAddress.of(type, url, branch, sourceType);
        }

        // Step 2. Sync blueprint library
        // (only when it's not default blueprint library address)
        BlueprintLibrary blueprintLibrary;
        if (blueprintLibraryAddressService.isDefaultBlueprintLibraryAddress(blueprintLibraryAddress)) {
            blueprintLibrary = blueprintLibraryService.getBlueprintLibrary(blueprintLibraryAddress.getType().name(), blueprintLibraryAddress.getUrl(), blueprintLibraryAddress.getBranch());
            if (blueprintLibrary == null) {
                throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_DEFAULT_NOT_FOUND).build();
            }
        } else {
            blueprintLibrary = blueprintLibrarySyncer.sync(blueprintLibraryAddress);
            if (blueprintLibrary == null) {
                throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_BEING_SYNCED).build();
            }
        }

        // Step 3. Switch blueprint library
        BlueprintLibrarySubscription blueprintLibrarySubscription = blueprintLibrarySubscriptionService.findByLibraryId(blueprintLibrary.getId());
        if (blueprintLibrarySubscription == null) {
            blueprintLibrarySubscription = BlueprintLibrarySubscription.builder()
                    .libraryId(blueprintLibrary.getId())
                    .libraryVersion(Objects.requireNonNullElse(blueprintLibrary.getCurrentVersion(), ""))
                    .active(false)
                    .build();
            blueprintLibrarySubscriptionService.save(blueprintLibrarySubscription);
        }
        blueprintLibrarySubscriptionService.setActiveOnlyByLibraryId(blueprintLibrary.getId());
        if (BlueprintLibrarySourceType.UPLOAD.name().equals(sourceType)) {
            tryLinkResource(blueprintLibraryAddress);
        }

        blueprintLibrarySyncer.notifyListenersWithTenant(blueprintLibrary, TenantContext.getTenantId());

        return ResponseBuilder.success();
    }

    private boolean validateSourceType(String sourceType) {
        return BlueprintLibrarySourceType.DEFAULT.name().equals(sourceType) || BlueprintLibrarySourceType.UPLOAD.name().equals(sourceType);
    }

    private void tryLinkResource(BlueprintLibraryAddress blueprintLibraryAddress) {
        if (blueprintLibraryAddress.getSourceType() == BlueprintLibrarySourceType.UPLOAD) {
            try {
                ResourceRefDTO resourceRefDTO = new ResourceRefDTO(blueprintLibraryAddress.getKey(), ResourceRefType.BLUEPRINT_LIBRARY_ADDRESS.name());
                resourceManagerFacade.linkByUrl(blueprintLibraryAddress.getUrl(), resourceRefDTO);
            } catch (Exception e) {
                log.warn("Try link url {} to resource failed.", blueprintLibraryAddress.getUrl());
            }
        }
    }
}
