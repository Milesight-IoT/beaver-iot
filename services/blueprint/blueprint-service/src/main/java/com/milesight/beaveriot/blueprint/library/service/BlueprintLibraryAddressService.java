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
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        List<BlueprintLibrarySubscription> allTenantsBlueprintLibrarySubscriptions = blueprintLibrarySubscriptionService.findAllIgnoreTenant();
        List<BlueprintLibraryAddress> allTenantsBlueprintLibraryAddresses = convertSubscriptionsToAddresses(allTenantsBlueprintLibrarySubscriptions);
        BlueprintLibraryAddress defaultBlueprintLibraryAddress = blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
        List<BlueprintLibraryAddress> allBlueprintLibraryAddresses = new ArrayList<>(allTenantsBlueprintLibraryAddresses);
        allBlueprintLibraryAddresses.add(defaultBlueprintLibraryAddress);

        Set<String> keys = new HashSet<>();
        for (BlueprintLibraryAddress blueprintLibraryAddress : allBlueprintLibraryAddresses) {
            if (!blueprintLibraryAddress.logicEquals(defaultBlueprintLibraryAddress) && !blueprintLibraryAddress.getActive()) {
                continue;
            }

            if (!keys.contains(blueprintLibraryAddress.getKey())) {
                keys.add(blueprintLibraryAddress.getKey());
                distinctBlueprintLibraryAddresses.add(blueprintLibraryAddress);
            }
        }
        return distinctBlueprintLibraryAddresses;
    }

    public boolean isDefaultBlueprintLibraryAddress(BlueprintLibraryAddress blueprintLibraryAddress) {
        return blueprintLibraryConfig.getDefaultBlueprintLibraryAddress().logicEquals(blueprintLibraryAddress);
    }

    private List<BlueprintLibraryAddress> convertSubscriptionsToAddresses(List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions) {
        if (CollectionUtils.isEmpty(blueprintLibrarySubscriptions)) {
            return Collections.emptyList();
        }

        List<BlueprintLibraryAddress> blueprintLibraryAddresses = new ArrayList<>();
        Map<Long, BlueprintLibrary> blueprintLibraryCache = new HashMap<>();
        for (BlueprintLibrarySubscription blueprintLibrarySubscription : blueprintLibrarySubscriptions) {
            if (!blueprintLibrarySubscription.getActive()) {
                continue;
            }

            BlueprintLibrary blueprintLibrary = blueprintLibraryCache.computeIfAbsent(blueprintLibrarySubscription.getLibraryId(), blueprintLibraryService::findById);
            BlueprintLibraryAddress blueprintLibraryAddress = convertLibraryToAddress(blueprintLibrary);
            blueprintLibraryAddresses.add(blueprintLibraryAddress);
        }
        return blueprintLibraryAddresses;
    }

    private BlueprintLibraryAddress convertSubscriptionToAddress(BlueprintLibrarySubscription blueprintLibrarySubscription) {
        BlueprintLibrary blueprintLibrary = blueprintLibraryService.findById(blueprintLibrarySubscription.getLibraryId());
        return convertLibraryToAddress(blueprintLibrary);
    }

    private BlueprintLibraryAddress convertLibraryToAddress(BlueprintLibrary blueprintLibrary) {
        return BlueprintLibraryAddress.of(blueprintLibrary.getType().name(), blueprintLibrary.getUrl(), blueprintLibrary.getBranch(), blueprintLibrary.getSourceType().name());
    }

    public BlueprintLibraryAddress getDefaultBlueprintLibraryAddress() {
        return blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
    }

    public BlueprintLibraryAddress getCurrentBlueprintLibraryAddress() {
        BlueprintLibraryAddress activeBlueprintLibraryAddress = findByActiveTrue();
        if (activeBlueprintLibraryAddress == null) {
            activeBlueprintLibraryAddress = blueprintLibraryConfig.getDefaultBlueprintLibraryAddress();
        }
        return activeBlueprintLibraryAddress;
    }

    public BlueprintLibraryAddress findByActiveTrue() {
        BlueprintLibrarySubscription blueprintLibrarySubscription = blueprintLibrarySubscriptionService.findByActiveTrue();
        if (blueprintLibrarySubscription == null) {
            return null;
        }

        return convertSubscriptionToAddress(blueprintLibrarySubscription);
    }

    public BlueprintLibraryManifest validateAndGetManifest(BlueprintLibraryAddress blueprintLibraryAddress) {
        if (blueprintLibraryAddress == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_NULL).build();
        }

        blueprintLibraryAddress.validate();

        String manifestContent;
        if (BlueprintLibraryType.Zip == blueprintLibraryAddress.getType()) {
            manifestContent = getManifestContentFromZip(blueprintLibraryAddress.getCodeZipUrl(), blueprintLibraryAddress.getManifestFilePath());
        } else {
            String manifestUrl = blueprintLibraryAddress.getRawManifestUrl();
            manifestContent = getManifestContentFromUrl(manifestUrl);
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

    private String getManifestContentFromZip(String zipUrl, String manifestFilePath) {
        Request request = new Request.Builder().url(zipUrl).build();
        try (Response response = OkHttpUtil.getClient().newCall(request).execute();
             InputStream inputStream = response.body() != null ? response.body().byteStream() : null) {
            if (inputStream == null) {
                return null;
            }
            try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                if (!response.isSuccessful()) {
                    return null;
                }

                ZipEntry rootEntry = zipInputStream.getNextEntry();
                if (rootEntry == null) {
                    return null;
                }

                String rootPrefix = rootEntry.getName();
                if (!rootEntry.isDirectory() || !rootPrefix.endsWith("/")) {
                    return null;
                }
                zipInputStream.closeEntry();

                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zipInputStream.closeEntry();
                        continue;
                    }

                    String entryName = entry.getName();
                    if (!entryName.startsWith(rootPrefix)) {
                        zipInputStream.closeEntry();
                        continue;
                    }

                    String relativePath = entryName.substring(rootPrefix.length());
                    if (relativePath.isEmpty()) {
                        zipInputStream.closeEntry();
                        continue;
                    }

                    byte[] bytes = zipInputStream.readAllBytes();
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    zipInputStream.closeEntry();
                    if (relativePath.equals(manifestFilePath)) {
                        return content;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}