package com.milesight.beaveriot.blueprint.library.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.library.component.BlueprintLibrarySyncer;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.request.SaveBlueprintLibrarySettingRequest;
import com.milesight.beaveriot.blueprint.library.model.response.QueryBlueprintLibrarySettingResponse;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryAddressService;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/17 15:05
 **/
@RestController
@RequestMapping("/blueprint-library-setting")
public class BlueprintLibrarySettingController {
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;
    private final BlueprintLibrarySyncer blueprintLibrarySyncer;

    public BlueprintLibrarySettingController(BlueprintLibraryAddressService blueprintLibraryAddressService, BlueprintLibrarySyncer blueprintLibrarySyncer) {
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.blueprintLibrarySyncer = blueprintLibrarySyncer;
    }

    @GetMapping("")
    public ResponseBody<QueryBlueprintLibrarySettingResponse> getBlueprintLibrarySetting() {
        BlueprintLibraryAddress defaultBlueprintLibraryAddress = blueprintLibraryAddressService.getDefaultBlueprintLibraryAddress();

        List<BlueprintLibraryAddress> blueprintLibraryAddresses = blueprintLibraryAddressService.findAll();
        if (CollectionUtils.isEmpty(blueprintLibraryAddresses)) {
            defaultBlueprintLibraryAddress.setActive(true);
            return ResponseBuilder.success(QueryBlueprintLibrarySettingResponse.of(BlueprintLibrarySourceType.Default.name(),
                    Map.of(BlueprintLibrarySourceType.Default.name(), List.of(defaultBlueprintLibraryAddress))));
        }

        Map<BlueprintLibraryType, BlueprintLibraryAddress> typeAddressMap = new HashMap<>();
        for (BlueprintLibraryAddress blueprintLibraryAddress : blueprintLibraryAddresses) {
            BlueprintLibraryAddress existBlueprintLibraryAddress = typeAddressMap.get(blueprintLibraryAddress.getType());
            if (existBlueprintLibraryAddress == null || blueprintLibraryAddress.getCreatedAt() > existBlueprintLibraryAddress.getCreatedAt()) {
                typeAddressMap.put(blueprintLibraryAddress.getType(), blueprintLibraryAddress);
            }
        }

        Map<String, List<BlueprintLibraryAddress>> switchTypeAddressMap = new HashMap<>();
        String currentSourceType = null;
        for (BlueprintLibraryType type : typeAddressMap.keySet()) {
            String sourceType = switch(type) {
                case Github, Gitlab -> BlueprintLibrarySourceType.Custom.name();
                case Zip -> BlueprintLibrarySourceType.Zip.name();
            };

            BlueprintLibraryAddress blueprintLibraryAddress = typeAddressMap.get(type);
            if (blueprintLibraryAddress.getActive()) {
                currentSourceType = sourceType;
            }
            List<BlueprintLibraryAddress> addresses = switchTypeAddressMap.computeIfAbsent(sourceType, k -> new ArrayList<>());
            addresses.add(blueprintLibraryAddress);
        }

        if (currentSourceType == null) {
            currentSourceType = BlueprintLibrarySourceType.Default.name();
            defaultBlueprintLibraryAddress.setActive(true);
        }
        switchTypeAddressMap.put(BlueprintLibrarySourceType.Default.name(), List.of(defaultBlueprintLibraryAddress));
        return ResponseBuilder.success(QueryBlueprintLibrarySettingResponse.of(currentSourceType, switchTypeAddressMap));
    }

    @PostMapping("")
    public ResponseBody<Void> saveBlueprintLibrarySetting(@RequestBody SaveBlueprintLibrarySettingRequest request) throws Exception {
        String sourceType = request.getSourceType();
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
        }

        return ResponseBuilder.success();
    }
}
