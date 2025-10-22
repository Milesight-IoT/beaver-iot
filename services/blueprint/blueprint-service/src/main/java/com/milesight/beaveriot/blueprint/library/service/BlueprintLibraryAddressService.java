package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.library.config.BlueprintLibraryConfig;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryManifest;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibrarySubscription;
import com.milesight.beaveriot.blueprint.library.support.YamlConverter;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * author: Luxb
 * create: 2025/9/1 10:04
 **/
@Slf4j
@Service
public class BlueprintLibraryAddressService {
    private final BlueprintLibraryConfig blueprintLibraryConfig;
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService;

    public BlueprintLibraryAddressService(BlueprintLibraryConfig blueprintLibraryConfig,
                                          @Lazy BlueprintLibraryService blueprintLibraryService,
                                          BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService) {
        this.blueprintLibraryConfig = blueprintLibraryConfig;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibrarySubscriptionService = blueprintLibrarySubscriptionService;
    }

    public List<BlueprintLibraryAddress> getDistinctBlueprintLibraryAddresses() {
        List<BlueprintLibraryAddress> distinctBlueprintLibraryAddresses = new ArrayList<>();

        List<BlueprintLibrarySubscription> allTenantsActiveBlueprintLibrarySubscriptions = blueprintLibrarySubscriptionService.findAllByActiveTrueIgnoreTenant();
        List<Long> activeBlueprintLibraryIds = getActiveBlueprintLibraryIds(allTenantsActiveBlueprintLibrarySubscriptions);
        List<BlueprintLibraryAddress> allTenantsActiveBlueprintLibraryAddresses = convertLibraryIdsToAddresses(activeBlueprintLibraryIds);
        BlueprintLibraryAddress defaultBlueprintLibraryAddress = getDefaultBlueprintLibraryAddress();
        List<BlueprintLibraryAddress> allBlueprintLibraryAddresses = new ArrayList<>();
        allBlueprintLibraryAddresses.add(defaultBlueprintLibraryAddress);
        if (!CollectionUtils.isEmpty(allTenantsActiveBlueprintLibraryAddresses)) {
            for (BlueprintLibraryAddress blueprintLibraryAddress : allTenantsActiveBlueprintLibraryAddresses) {
                if (blueprintLibraryAddress.getSourceType() == BlueprintLibrarySourceType.DEFAULT && !isDefaultBlueprintLibraryAddress(blueprintLibraryAddress)) {
                    continue;
                }
                allBlueprintLibraryAddresses.add(blueprintLibraryAddress);
            }
        }

        Set<String> keys = new HashSet<>();
        for (BlueprintLibraryAddress blueprintLibraryAddress : allBlueprintLibraryAddresses) {
            if (!keys.contains(blueprintLibraryAddress.getKey())) {
                keys.add(blueprintLibraryAddress.getKey());
                distinctBlueprintLibraryAddresses.add(blueprintLibraryAddress);
            }
        }
        return distinctBlueprintLibraryAddresses;
    }

    private List<Long> getActiveBlueprintLibraryIds(List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions) {
        Set<Long> activeLibraryIds = new TreeSet<>();
        for (BlueprintLibrarySubscription blueprintLibrarySubscription : blueprintLibrarySubscriptions) {
            if (blueprintLibrarySubscription.getActive()) {
                activeLibraryIds.add(blueprintLibrarySubscription.getLibraryId());
            }
        }
        return new ArrayList<>(activeLibraryIds);
    }

    public boolean isDefaultBlueprintLibraryAddress(BlueprintLibraryAddress blueprintLibraryAddress) {
        return blueprintLibraryConfig.getDefaultBlueprintLibraryAddress().logicEquals(blueprintLibraryAddress);
    }

    private List<BlueprintLibraryAddress> convertLibraryIdsToAddresses(List<Long> blueprintLibraryIds) {
        if (CollectionUtils.isEmpty(blueprintLibraryIds)) {
            return Collections.emptyList();
        }

        List<BlueprintLibraryAddress> blueprintLibraryAddresses = new ArrayList<>();
        for (Long blueprintLibraryId : blueprintLibraryIds) {
            BlueprintLibrary blueprintLibrary = blueprintLibraryService.findById(blueprintLibraryId);
            BlueprintLibraryAddress blueprintLibraryAddress = convertLibraryToAddress(blueprintLibrary);
            blueprintLibraryAddresses.add(blueprintLibraryAddress);
        }
        return blueprintLibraryAddresses;
    }

    private BlueprintLibraryAddress convertSubscriptionToAddress(BlueprintLibrarySubscription blueprintLibrarySubscription) {
        BlueprintLibrary blueprintLibrary = blueprintLibraryService.findById(blueprintLibrarySubscription.getLibraryId());
        return convertLibraryToAddress(blueprintLibrary);
    }

    public BlueprintLibraryAddress convertLibraryToAddress(BlueprintLibrary blueprintLibrary) {
        return BlueprintLibraryAddress.of(blueprintLibrary.getType().name(), blueprintLibrary.getUrl(), blueprintLibrary.getBranch(), blueprintLibrary.getSourceType().name());
    }

    public BlueprintLibraryAddress getDefaultBlueprintLibraryAddress() {
        return blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
    }

    public BlueprintLibraryAddress getCurrentBlueprintLibraryAddress() {
        BlueprintLibraryAddress activeBlueprintLibraryAddress = findByActiveTrue();
        if (activeBlueprintLibraryAddress == null) {
            activeBlueprintLibraryAddress = blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
        } else {
            if (activeBlueprintLibraryAddress.getSourceType() == BlueprintLibrarySourceType.DEFAULT && !isDefaultBlueprintLibraryAddress(activeBlueprintLibraryAddress)) {
                activeBlueprintLibraryAddress = blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
            }
        }
        return activeBlueprintLibraryAddress;
    }

    public BlueprintLibraryAddress findByActiveTrue() {
        BlueprintLibrarySubscription blueprintLibrarySubscription = blueprintLibrarySubscriptionService.findByActiveTrue();
        if (blueprintLibrarySubscription == null) {
            return null;
        }

        BlueprintLibraryAddress address = convertSubscriptionToAddress(blueprintLibrarySubscription);
        address.setActive(blueprintLibrarySubscription.getActive());
        return address;
    }

    public BlueprintLibraryManifest validateAndGetManifest(BlueprintLibraryAddress blueprintLibraryAddress) {
        if (blueprintLibraryAddress == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_NULL).build();
        }

        blueprintLibraryAddress.validate();

        String manifestContent = blueprintLibraryAddress.getManifestContent();
        if (manifestContent == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_NOT_REACHABLE).build();
        }

        BlueprintLibraryManifest manifest = YamlConverter.from(manifestContent, BlueprintLibraryManifest.class);
        if (manifest == null || !manifest.validate()) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_INVALID).build();
        }

        return manifest;
    }
}