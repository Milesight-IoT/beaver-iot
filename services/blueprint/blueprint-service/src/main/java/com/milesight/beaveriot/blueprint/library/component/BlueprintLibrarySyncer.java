package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import com.milesight.beaveriot.base.exception.MultipleErrorException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.library.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryErrorCode;
import com.milesight.beaveriot.blueprint.library.model.*;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryResourceService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryAddressService;
import com.milesight.beaveriot.context.application.ApplicationProperties;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * author: Luxb
 * create: 2025/9/1 13:44
 **/
@Slf4j
@Service
public class BlueprintLibrarySyncer {
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibraryResourceService blueprintLibraryResourceService;
    private final BlueprintLibraryResourceResolver blueprintLibraryResourceResolver;
    private final ApplicationProperties applicationProperties;

    public BlueprintLibrarySyncer(BlueprintLibraryAddressService blueprintLibraryAddressService, BlueprintLibraryService blueprintLibraryService, BlueprintLibraryResourceService blueprintLibraryResourceService, BlueprintLibraryResourceResolver blueprintLibraryResourceResolver, ApplicationProperties applicationProperties) {
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibraryResourceService = blueprintLibraryResourceService;
        this.blueprintLibraryResourceResolver = blueprintLibraryResourceResolver;
        this.applicationProperties = applicationProperties;
    }

    @DistributedLock(name = "blueprint-library-sync-#{#p0.key}", waitForLock = "1s", scope = LockScope.GLOBAL)
    public void sync(BlueprintLibraryAddress blueprintLibraryAddress) {
        long start = System.currentTimeMillis();

        log.debug("Start checking blueprint library {}", blueprintLibraryAddress.getKey());
        BlueprintLibraryAddressValidationResult validationResult = blueprintLibraryAddressService.validate(blueprintLibraryAddress);
        if (!validationResult.getErrors().isEmpty()) {
            throw MultipleErrorException.with(HttpStatus.BAD_REQUEST.value(), "Validate blueprint library address error", validationResult.getErrors());
        }

        BlueprintLibraryManifest manifest = validationResult.getManifest();
        BlueprintLibrary blueprintLibrary = blueprintLibraryService.getBlueprintLibrary(blueprintLibraryAddress.getHome(), blueprintLibraryAddress.getBranch());
        if (!isNeedUpdateLibrary(blueprintLibrary, manifest.getVersion())) {
            log.debug("Skipping update for blueprint library {} because it is already up to date", blueprintLibraryAddress.getKey());
            return;
        }

        log.debug("Found new version: {} for blueprint library {}", manifest.getVersion(), blueprintLibraryAddress.getKey());
        String currentBeaverVersion = getCurrentBeaverVersion();
        if (!isBeaverVersionSupported(currentBeaverVersion, manifest.getMinimumRequiredBeaverIoTVersion())) {
            if (blueprintLibrary == null) {
                blueprintLibrary = BlueprintLibrary.builder()
                        .home(blueprintLibraryAddress.getHome())
                        .branch(blueprintLibraryAddress.getBranch())
                        .remoteVersion(manifest.getVersion())
                        .build();
            }
            if (blueprintLibrary.getCurrentVersion() == null) {
                throwSyncFailedExceptionAndUpdateBlueprintLibrary(blueprintLibrary, BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_BEAVER_VERSION_UNSUPPORTED);
            }
            log.debug("Skipping update for blueprint library {} because current beaver version {} is below minimum required version {}",
                    blueprintLibraryAddress.getKey(),
                    currentBeaverVersion,
                    manifest.getMinimumRequiredBeaverIoTVersion());
            return;
        }

        doSync(blueprintLibrary, manifest, blueprintLibraryAddress, start);
    }

    private void doSync(BlueprintLibrary blueprintLibrary, BlueprintLibraryManifest manifest, BlueprintLibraryAddress blueprintLibraryAddress, long start) {
        log.debug("Start syncing blueprint library: {}", blueprintLibraryAddress.getKey());

        if (blueprintLibrary == null) {
            blueprintLibrary = BlueprintLibrary.builder()
                    .home(blueprintLibraryAddress.getHome())
                    .branch(blueprintLibraryAddress.getBranch())
                    .build();
        }
        blueprintLibrary.setSyncedAt(System.currentTimeMillis());
        blueprintLibrary.setRemoteVersion(manifest.getVersion());
        blueprintLibrary.setSyncStatus(BlueprintLibrarySyncStatus.SYNCING);
        blueprintLibraryService.save(blueprintLibrary);

        String codeZipUrl = blueprintLibraryAddress.getCodeZipUrl();
        if (StringUtils.isEmpty(codeZipUrl)) {
            throwSyncFailedExceptionAndUpdateBlueprintLibrary(blueprintLibrary, BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_SYNC_FAILED);
            return;
        }

        List<BlueprintLibraryResource> blueprintLibraryResources = new ArrayList<>();
        Request request = new Request.Builder().url(codeZipUrl).build();
        try (Response response = OkHttpUtil.getClient().newCall(request).execute();
             InputStream inputStream = response.body() != null ? response.body().byteStream() : null) {
            if (inputStream == null) {
                throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_SYNC_FAILED).build();
            }
            try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                if (!response.isSuccessful()) {
                    throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_SYNC_FAILED).build();
                }

                ZipEntry rootEntry = zipInputStream.getNextEntry();
                if (rootEntry == null) {
                    throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_SYNC_FAILED).build();
                }

                String rootPrefix = rootEntry.getName();
                if (!rootEntry.isDirectory() || !rootPrefix.endsWith("/")) {
                    throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_SYNC_FAILED).build();
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
                    BlueprintLibraryResource blueprintLibraryResource = BlueprintLibraryResource.builder()
                            .path(relativePath)
                            .content(content)
                            .libraryId(blueprintLibrary.getId())
                            .libraryVersion(manifest.getVersion())
                            .build();
                    blueprintLibraryResources.add(blueprintLibraryResource);
                    zipInputStream.closeEntry();
                }
            }

            if (blueprintLibraryResources.isEmpty()) {
                throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_SYNC_FAILED).build();
            }

            blueprintLibraryResourceService.deleteAllByLibraryIdAndLibraryVersion(blueprintLibrary.getId(), manifest.getVersion());
            blueprintLibraryResourceService.batchSave(blueprintLibraryResources);

            BlueprintLibrary oldBlueprintLibrary = BlueprintLibrary.clone(blueprintLibrary);
            blueprintLibrary.setCurrentVersion(manifest.getVersion());
            blueprintLibrary.setSyncedAt(System.currentTimeMillis());
            blueprintLibrary.setSyncStatus(BlueprintLibrarySyncStatus.SYNCED);
            blueprintLibraryService.save(blueprintLibrary);

            evictCaches(oldBlueprintLibrary);
        } catch (IOException e) {
            throwSyncFailedExceptionAndUpdateBlueprintLibrary(blueprintLibrary, BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_SYNC_FAILED);
        }

        log.debug("Fishing syncing blueprint library: {}, time: {} ms", blueprintLibraryAddress.getKey(), System.currentTimeMillis() - start);
    }

    private void evictCaches(BlueprintLibrary blueprintLibrary) {
        List<BlueprintDeviceVendor> deviceVendors = blueprintLibraryResourceResolver.getDeviceVendors();
        blueprintLibraryService.evictCacheBlueprintLibrary(blueprintLibrary.getHome(), blueprintLibrary.getBranch());
        blueprintLibraryResourceResolver.evictCacheDeviceVendors(blueprintLibrary);
        if (!CollectionUtils.isEmpty(deviceVendors)) {
            deviceVendors.forEach(vendor -> blueprintLibraryResourceResolver.evictCacheDevices(blueprintLibrary, vendor.getId()));
        }
    }

    private void throwSyncFailedExceptionAndUpdateBlueprintLibrary(BlueprintLibrary blueprintLibrary, ErrorCodeSpec errorCodeSpec) throws ServiceException {
        blueprintLibrary.setSyncStatus(BlueprintLibrarySyncStatus.SYNC_FAILED);
        blueprintLibrary.setSyncedAt(System.currentTimeMillis());
        blueprintLibraryService.save(blueprintLibrary);
        throw ServiceException.with(errorCodeSpec).build();
    }

    private String getCurrentBeaverVersion() {
        return applicationProperties.getVersion();
    }

    private boolean isNeedUpdateLibrary(BlueprintLibrary blueprintLibrary, String remoteVersion) {
        if (blueprintLibrary == null) {
            return true;
        }

        String currentVersion = blueprintLibrary.getCurrentVersion();
        if (StringUtils.isEmpty(currentVersion) && !StringUtils.isEmpty(remoteVersion)) {
            return true;
        }

        if (StringUtils.isEmpty(remoteVersion)) {
            return false;
        }

        String[] currentParts = currentVersion.split("\\.");
        String[] remoteParts = remoteVersion.split("\\.");

        if (currentParts.length != 2 || remoteParts.length != 2) {
            return false;
        }

        try {
            int currentMajor = Integer.parseInt(currentParts[0]);
            int currentMinor = Integer.parseInt(currentParts[1]);
            int remoteMajor = Integer.parseInt(remoteParts[0]);
            int remoteMinor = Integer.parseInt(remoteParts[1]);

            if (remoteMajor > currentMajor) {
                return true;
            }
            if (remoteMajor < currentMajor) {
                return false;
            }
            return remoteMinor > currentMinor;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isBeaverVersionSupported(String currentBeaverVersion, String minimumBeaverVersion) {
        if (currentBeaverVersion == null || minimumBeaverVersion == null) {
            return false;
        }

        String[] currentVersions = currentBeaverVersion.split("\\.");
        String[] minimumVersions = minimumBeaverVersion.split("\\.");

        if (currentVersions.length != 3 || minimumVersions.length != 3) {
            return false;
        }

        try {
            for (int i = 0; i < 3; i++) {
                int currentVersionPart = Integer.parseInt(currentVersions[i]);
                int minimumVersionPart = Integer.parseInt(minimumVersions[i]);
                if (currentVersionPart < minimumVersionPart) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}