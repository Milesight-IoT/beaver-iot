package com.milesight.beaveriot.blueprint.library.controller;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.library.component.BlueprintLibrarySyncer;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.request.SaveBlueprintLibrarySettingRequest;
import com.milesight.beaveriot.blueprint.library.model.response.QueryBlueprintLibrarySettingResponse;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryAddressService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryService;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import com.milesight.beaveriot.resource.manager.dto.ResourceRefDTO;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * author: Luxb
 * create: 2025/9/17 15:05
 **/
@Slf4j
@RestController
@RequestMapping("/blueprint-library-setting")
public class BlueprintLibrarySettingController {
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibrarySyncer blueprintLibrarySyncer;
    private final ResourceManagerFacade resourceManagerFacade;

    public BlueprintLibrarySettingController(BlueprintLibraryAddressService blueprintLibraryAddressService, BlueprintLibraryService blueprintLibraryService, BlueprintLibrarySyncer blueprintLibrarySyncer, ResourceManagerFacade resourceManagerFacade) {
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibrarySyncer = blueprintLibrarySyncer;
        this.resourceManagerFacade = resourceManagerFacade;
    }

    @GetMapping("")
    public ResponseBody<QueryBlueprintLibrarySettingResponse> getBlueprintLibrarySetting() {
        QueryBlueprintLibrarySettingResponse response = new QueryBlueprintLibrarySettingResponse();
        BlueprintLibraryAddress defaultBlueprintLibraryAddress = blueprintLibraryAddressService.getDefaultBlueprintLibraryAddress();

        BlueprintLibraryAddress activeBlueprintLibraryAddress = blueprintLibraryAddressService.findByActiveTrue();
        if (activeBlueprintLibraryAddress != null && activeBlueprintLibraryAddress.getType() == BlueprintLibraryType.Zip) {
            BlueprintLibrary activeBlueprintLibrary = blueprintLibraryService.getBlueprintLibrary(activeBlueprintLibraryAddress.getType().name(), activeBlueprintLibraryAddress.getUrl(), activeBlueprintLibraryAddress.getBranch());
            response.setCurrentSourceType(BlueprintLibrarySourceType.Upload.name());
            if (activeBlueprintLibrary != null) {
                response.setVersion(activeBlueprintLibrary.getCurrentVersion());
                response.setFileName(getZipFileFromUrl(activeBlueprintLibrary.getUrl()));
            }
        } else {
            BlueprintLibrary defaultBlueprintLibrary = blueprintLibraryService.getBlueprintLibrary(defaultBlueprintLibraryAddress.getType().name(), defaultBlueprintLibraryAddress.getUrl(), defaultBlueprintLibraryAddress.getBranch());
            response.setCurrentSourceType(BlueprintLibrarySourceType.Default.name());
            if (defaultBlueprintLibrary != null) {
                response.setVersion(defaultBlueprintLibrary.getCurrentVersion());
                response.setUpdateTime(defaultBlueprintLibrary.getSyncedAt());
            }
        }

        return ResponseBuilder.success(response);
    }

    private String getZipFileFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

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
        if (BlueprintLibrarySourceType.Default.name().equals(sourceType)) {
            blueprintLibraryAddress = blueprintLibraryAddressService.getDefaultBlueprintLibraryAddress();
        } else {
            blueprintLibraryAddress = blueprintLibraryAddressService.findByTypeAndUrlAndBranch(type, url, branch);
            if (blueprintLibraryAddress == null) {
                blueprintLibraryAddress = BlueprintLibraryAddress.of(type, url, branch);
                blueprintLibraryAddress.setId(SnowflakeUtil.nextId());
            }
        }

        // Step 2. Sync blueprint library
        blueprintLibrarySyncer.sync(blueprintLibraryAddress);

        // Step 3. Switch blueprint library
        if (BlueprintLibrarySourceType.Default.name().equals(sourceType)) {
            blueprintLibraryAddressService.setAllInactive();
        } else {
            blueprintLibraryAddress.setActive(true);
            blueprintLibraryAddressService.save(blueprintLibraryAddress);
            blueprintLibraryAddressService.setActiveOnlyByTypeUrlBranch(type, url, branch);
            tryLinkResource(blueprintLibraryAddress);
        }

        return ResponseBuilder.success();
    }

    private boolean validateSourceType(String sourceType) {
        return BlueprintLibrarySourceType.Default.name().equals(sourceType) || BlueprintLibrarySourceType.Upload.name().equals(sourceType);
    }

    private void tryLinkResource(BlueprintLibraryAddress blueprintLibraryAddress) {
        if (blueprintLibraryAddress.getType() == BlueprintLibraryType.Zip) {
            try {
                ResourceRefDTO resourceRefDTO = new ResourceRefDTO(blueprintLibraryAddress.getKey(), BlueprintLibraryAddress.RESOURCE_TYPE);
                resourceManagerFacade.linkByUrl(blueprintLibraryAddress.getUrl(), resourceRefDTO);
            } catch (Exception e) {
                log.warn("Try link url {} to resource failed.", blueprintLibraryAddress.getUrl());
            }
        }
    }
}
