package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.library.client.response.ClientResponse;
import com.milesight.beaveriot.blueprint.library.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.library.config.BlueprintLibraryConfig;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryManifest;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibrarySubscription;
import com.milesight.beaveriot.blueprint.library.support.YamlConverter;
import com.milesight.beaveriot.blueprint.library.support.ZipInputStreamScanner;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
    private final ResourceManagerFacade resourceManagerFacade;

    public BlueprintLibraryAddressService(BlueprintLibraryConfig blueprintLibraryConfig,
                                          @Lazy BlueprintLibraryService blueprintLibraryService,
                                          BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService, ResourceManagerFacade resourceManagerFacade) {
        this.blueprintLibraryConfig = blueprintLibraryConfig;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibrarySubscriptionService = blueprintLibrarySubscriptionService;
        this.resourceManagerFacade = resourceManagerFacade;
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
            allBlueprintLibraryAddresses.addAll(allTenantsActiveBlueprintLibraryAddresses);
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

        String manifestContent;
        if (blueprintLibraryAddress.isProxyMode() && blueprintLibraryAddress.getProxy() != null) {
            manifestContent = blueprintLibraryAddress.getProxy().getManifestContent();
        } else {
            if (blueprintLibraryAddress.getSourceType() == BlueprintLibrarySourceType.UPLOAD) {
                manifestContent = getManifestContentFromResourceZip(blueprintLibraryAddress.getCodeZipUrl(), blueprintLibraryAddress.getManifestFilePath());
            } else {
                String manifestUrl = blueprintLibraryAddress.getRawManifestUrl();
                try {
                    manifestContent = getManifestContentFromUrl(manifestUrl);
                } catch (Exception e) {
                    if (blueprintLibraryAddress.getProxy() == null) {
                        throw e;
                    }
                    log.warn("Failed to access blueprint library {}: falling back to proxy mode", blueprintLibraryAddress.getKey());
                    manifestContent = blueprintLibraryAddress.switchAndGetProxy().getManifestContent();
                }
            }
        }

        if (manifestContent == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_NOT_REACHABLE).build();
        }

        BlueprintLibraryManifest manifest = YamlConverter.from(manifestContent, BlueprintLibraryManifest.class);
        if (manifest == null || !manifest.validate()) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_INVALID).build();
        }

        return manifest;
    }

    private String getManifestContentFromUrl(String manifestUrl) {
        ClientResponse response = OkHttpUtil.get(manifestUrl);
        if (response == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_ACCESS_FAILED).build();
        }

        if (!response.isSuccessful()) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_ACCESS_FAILED).build();
        }

        return response.getData();
    }

    private String getManifestContentFromResourceZip(String zipUrl, String manifestFilePath) {
        return getManifestContentFromZip(zipUrl, manifestFilePath, resourceManagerFacade::getDataByUrl);
    }

    public String getManifestContentFromZip(String zipUrl, String manifestFilePath, Function<String, InputStream> inputStreamFetcher) {
        try (InputStream inputStream = inputStreamFetcher.apply(zipUrl)) {
            AtomicReference<String> manifestContent = new AtomicReference<>();
            boolean isSuccess = ZipInputStreamScanner.scan(inputStream, (relativePath, content) -> {
                if (relativePath.equals(manifestFilePath)) {
                    manifestContent.set(content);
                    return false;
                } else {
                    return true;
                }
            });

            if (isSuccess) {
                return manifestContent.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}