package com.milesight.beaveriot.blueprint.component;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import com.milesight.beaveriot.base.exception.MultipleErrorException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.enums.BlueprintRepositoryErrorCode;
import com.milesight.beaveriot.blueprint.model.*;
import com.milesight.beaveriot.blueprint.service.BlueprintRepositoryAddressService;
import com.milesight.beaveriot.blueprint.service.BlueprintRepositoryResourceService;
import com.milesight.beaveriot.blueprint.service.BlueprintRepositoryService;
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
public class BlueprintRepositorySyncer {
    private final BlueprintRepositoryAddressService blueprintRepositoryAddressService;
    private final BlueprintRepositoryService blueprintRepositoryService;
    private final BlueprintRepositoryResourceService blueprintRepositoryResourceService;
    private final BlueprintRepositoryResourceResolver blueprintRepositoryResourceResolver;
    private final ApplicationProperties applicationProperties;

    public BlueprintRepositorySyncer(BlueprintRepositoryAddressService blueprintRepositoryAddressService, BlueprintRepositoryService blueprintRepositoryService, BlueprintRepositoryResourceService blueprintRepositoryResourceService, BlueprintRepositoryResourceResolver blueprintRepositoryResourceResolver, ApplicationProperties applicationProperties) {
        this.blueprintRepositoryAddressService = blueprintRepositoryAddressService;
        this.blueprintRepositoryService = blueprintRepositoryService;
        this.blueprintRepositoryResourceService = blueprintRepositoryResourceService;
        this.blueprintRepositoryResourceResolver = blueprintRepositoryResourceResolver;
        this.applicationProperties = applicationProperties;
    }

    @DistributedLock(name = "blueprint-repository-sync-#{#p0.key}", waitForLock = "1s", scope = LockScope.GLOBAL)
    public void sync(BlueprintRepositoryAddress blueprintRepositoryAddress) {
        long start = System.currentTimeMillis();

        log.debug("Start checking blueprint repository {}", blueprintRepositoryAddress.getKey());
        BlueprintRepositoryAddressValidationResult validationResult = blueprintRepositoryAddressService.validate(blueprintRepositoryAddress);
        if (!validationResult.getErrors().isEmpty()) {
            throw MultipleErrorException.with(HttpStatus.BAD_REQUEST.value(), "Validate blueprint repository address error", validationResult.getErrors());
        }

        BlueprintRepositoryManifest manifest = validationResult.getManifest();
        BlueprintRepository blueprintRepository = blueprintRepositoryService.getBlueprintRepository(blueprintRepositoryAddress.getHome(), blueprintRepositoryAddress.getBranch());
        if (!isNeedUpdateRepository(blueprintRepository, manifest.getVersion())) {
            log.debug("Skipping update for blueprint repository {} because it is already up to date", blueprintRepositoryAddress.getKey());
            return;
        }

        log.debug("Found new version: {} for blueprint repository {}", manifest.getVersion(), blueprintRepositoryAddress.getKey());
        String currentBeaverVersion = getCurrentBeaverVersion();
        if (!isBeaverVersionSupported(currentBeaverVersion, manifest.getMinimumRequiredBeaverIoTVersion())) {
            if (blueprintRepository == null) {
                blueprintRepository = BlueprintRepository.builder()
                        .home(blueprintRepositoryAddress.getHome())
                        .branch(blueprintRepositoryAddress.getBranch())
                        .remoteVersion(manifest.getVersion())
                        .build();
            }
            if (blueprintRepository.getCurrentVersion() == null) {
                throwSyncFailedExceptionAndUpdateBlueprintRepository(blueprintRepository, BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_BEAVER_VERSION_UNSUPPORTED);
            }
            log.debug("Skipping update for blueprint repository {} because current beaver version {} is below minimum required version {}",
                    blueprintRepositoryAddress.getKey(),
                    currentBeaverVersion,
                    manifest.getMinimumRequiredBeaverIoTVersion());
            return;
        }

        doSync(blueprintRepository, manifest, blueprintRepositoryAddress, start);
    }

    private void doSync(BlueprintRepository blueprintRepository, BlueprintRepositoryManifest manifest, BlueprintRepositoryAddress blueprintRepositoryAddress, long start) {
        log.debug("Start syncing blueprint repository: {}", blueprintRepositoryAddress.getKey());

        if (blueprintRepository == null) {
            blueprintRepository = BlueprintRepository.builder()
                    .home(blueprintRepositoryAddress.getHome())
                    .branch(blueprintRepositoryAddress.getBranch())
                    .build();
        }
        blueprintRepository.setSyncedAt(System.currentTimeMillis());
        blueprintRepository.setRemoteVersion(manifest.getVersion());
        blueprintRepository.setSyncStatus(BlueprintRepositorySyncStatus.SYNCING);
        blueprintRepositoryService.save(blueprintRepository);

        String codeZipUrl = blueprintRepositoryAddress.getCodeZipUrl();
        if (StringUtils.isEmpty(codeZipUrl)) {
            throwSyncFailedExceptionAndUpdateBlueprintRepository(blueprintRepository, BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_SYNC_FAILED);
            return;
        }

        List<BlueprintRepositoryResource> blueprintRepositoryResources = new ArrayList<>();
        Request request = new Request.Builder().url(codeZipUrl).build();
        try (Response response = OkHttpUtil.getClient().newCall(request).execute();
             InputStream inputStream = response.body() != null ? response.body().byteStream() : null) {
            if (inputStream == null) {
                throw ServiceException.with(BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_SYNC_FAILED).build();
            }
            try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                if (!response.isSuccessful()) {
                    throw ServiceException.with(BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_SYNC_FAILED).build();
                }

                ZipEntry rootEntry = zipInputStream.getNextEntry();
                if (rootEntry == null) {
                    throw ServiceException.with(BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_SYNC_FAILED).build();
                }

                String rootPrefix = rootEntry.getName();
                if (!rootEntry.isDirectory() || !rootPrefix.endsWith("/")) {
                    throw ServiceException.with(BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_SYNC_FAILED).build();
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
                    BlueprintRepositoryResource blueprintRepositoryResource = BlueprintRepositoryResource.builder()
                            .path(relativePath)
                            .content(content)
                            .repositoryId(blueprintRepository.getId())
                            .repositoryVersion(manifest.getVersion())
                            .build();
                    blueprintRepositoryResources.add(blueprintRepositoryResource);
                    zipInputStream.closeEntry();
                }
            }

            if (blueprintRepositoryResources.isEmpty()) {
                throw ServiceException.with(BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_SYNC_FAILED).build();
            }

            blueprintRepositoryResourceService.deleteAllByRepositoryIdAndRepositoryVersion(blueprintRepository.getId(), manifest.getVersion());
            blueprintRepositoryResourceService.batchSave(blueprintRepositoryResources);

            BlueprintRepository oldBlueprintRepository = BlueprintRepository.clone(blueprintRepository);
            blueprintRepository.setCurrentVersion(manifest.getVersion());
            blueprintRepository.setSyncedAt(System.currentTimeMillis());
            blueprintRepository.setSyncStatus(BlueprintRepositorySyncStatus.SYNCED);
            blueprintRepositoryService.save(blueprintRepository);

            evictCaches(oldBlueprintRepository);
        } catch (IOException e) {
            throwSyncFailedExceptionAndUpdateBlueprintRepository(blueprintRepository, BlueprintRepositoryErrorCode.BLUEPRINT_REPOSITORY_SYNC_FAILED);
        }

        log.debug("Fishing syncing blueprint repository: {}, time: {} ms", blueprintRepositoryAddress.getKey(), System.currentTimeMillis() - start);
    }

    private void evictCaches(BlueprintRepository blueprintRepository) {
        List<BlueprintDeviceVendor> deviceVendors = blueprintRepositoryResourceResolver.getDeviceVendors();
        if (!CollectionUtils.isEmpty(deviceVendors)) {
            deviceVendors.forEach(vendor -> blueprintRepositoryResourceResolver.evictCacheDevices(blueprintRepository, vendor.getId()));
        }
        blueprintRepositoryResourceResolver.evictCacheDeviceVendors(blueprintRepository);
        blueprintRepositoryService.evictCacheBlueprintRepository(blueprintRepository.getHome(), blueprintRepository.getBranch());
    }

    private void throwSyncFailedExceptionAndUpdateBlueprintRepository(BlueprintRepository blueprintRepository, ErrorCodeSpec errorCodeSpec) throws ServiceException {
        blueprintRepository.setSyncStatus(BlueprintRepositorySyncStatus.SYNC_FAILED);
        blueprintRepository.setSyncedAt(System.currentTimeMillis());
        blueprintRepositoryService.save(blueprintRepository);
        throw ServiceException.with(errorCodeSpec).build();
    }

    private String getCurrentBeaverVersion() {
        return applicationProperties.getVersion();
    }

    private boolean isNeedUpdateRepository(BlueprintRepository blueprintRepository, String remoteVersion) {
        if (blueprintRepository == null) {
            return true;
        }

        String currentVersion = blueprintRepository.getCurrentVersion();
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