package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.library.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryErrorCode;
import com.milesight.beaveriot.blueprint.library.model.*;
import com.milesight.beaveriot.blueprint.library.service.*;
import com.milesight.beaveriot.context.application.ApplicationProperties;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.model.BlueprintLibrarySyncStatus;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import com.milesight.beaveriot.user.facade.ITenantFacade;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
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
    private final BlueprintLibraryVersionService blueprintLibraryVersionService;
    private final BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService;
    private final BlueprintLibraryResourceService blueprintLibraryResourceService;
    private final BlueprintLibraryResourceResolver blueprintLibraryResourceResolver;
    private final ITenantFacade tenantFacade;
    private final ResourceManagerFacade resourceManagerFacade;
    private final ApplicationProperties applicationProperties;
    private final List<Consumer<BlueprintLibrary>> listeners;
    private static final ExecutorService listenerExecutor = Executors.newCachedThreadPool();

    public BlueprintLibrarySyncer(BlueprintLibraryAddressService blueprintLibraryAddressService,
                                  BlueprintLibraryService blueprintLibraryService,
                                  BlueprintLibraryVersionService blueprintLibraryVersionService,
                                  BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService,
                                  BlueprintLibraryResourceService blueprintLibraryResourceService,
                                  BlueprintLibraryResourceResolver blueprintLibraryResourceResolver,
                                  ITenantFacade tenantFacade, ResourceManagerFacade resourceManagerFacade,
                                  ApplicationProperties applicationProperties) {
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibraryVersionService = blueprintLibraryVersionService;
        this.blueprintLibrarySubscriptionService = blueprintLibrarySubscriptionService;
        this.blueprintLibraryResourceService = blueprintLibraryResourceService;
        this.blueprintLibraryResourceResolver = blueprintLibraryResourceResolver;
        this.tenantFacade = tenantFacade;
        this.resourceManagerFacade = resourceManagerFacade;
        this.applicationProperties = applicationProperties;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @DistributedLock(name = "blueprint-library-sync-#{#p0.key}", waitForLock = "10s", scope = LockScope.GLOBAL, throwOnLockFailure = false)
    public BlueprintLibrary sync(BlueprintLibraryAddress blueprintLibraryAddress) throws Exception {
        long start = System.currentTimeMillis();

        log.debug("Start checking blueprint library {}", blueprintLibraryAddress.getKey());
        BlueprintLibrary blueprintLibrary = blueprintLibraryService.getBlueprintLibrary(blueprintLibraryAddress.getType().name(), blueprintLibraryAddress.getUrl(), blueprintLibraryAddress.getBranch());
        if (blueprintLibrary == null) {
            blueprintLibrary = BlueprintLibrary.builder()
                    .type(blueprintLibraryAddress.getType())
                    .url(blueprintLibraryAddress.getUrl())
                    .branch(blueprintLibraryAddress.getBranch())
                    .sourceType(blueprintLibraryAddress.getSourceType())
                    .build();
        }

        if (blueprintLibrary.getSourceType() == BlueprintLibrarySourceType.Upload && blueprintLibrary.getSyncStatus() == BlueprintLibrarySyncStatus.SYNCED) {
            syncDoneWithMessage(blueprintLibrary,
                    MessageFormat.format("Skipping update for blueprint library {0} because it is already up to date", blueprintLibraryAddress.getKey()));
            return blueprintLibrary;
        }

        if (BlueprintLibrarySyncStatus.SYNCING != blueprintLibrary.getSyncStatus()) {
            blueprintLibrary.setSyncStatus(BlueprintLibrarySyncStatus.SYNCING);
            blueprintLibrary.setSyncMessage("Start checking blueprint library");
            blueprintLibraryService.save(blueprintLibrary);
        }

        try {
            BlueprintLibraryManifest manifest = blueprintLibraryAddressService.validateAndGetManifest(blueprintLibraryAddress);
            if (!isNeedUpdateLibrary(blueprintLibrary, manifest.getVersion())) {
                syncDoneWithMessage(blueprintLibrary,
                        MessageFormat.format("Skipping update for blueprint library {0} because it is already up to date", blueprintLibraryAddress.getKey()));
                switchDefaultBlueprintLibrarySubscriptionForAllTenantsIfNeeded(blueprintLibrary, blueprintLibraryAddress);
                return blueprintLibrary;
            }

            log.debug("Found new version: {} for blueprint library {}", manifest.getVersion(), blueprintLibraryAddress.getKey());
            String currentBeaverVersion = getCurrentBeaverVersion();
            if (!isBeaverVersionSupported(currentBeaverVersion, manifest.getMinimumRequiredBeaverIotVersion())) {
                if (blueprintLibrary.getCurrentVersion() == null) {
                    throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_BEAVER_VERSION_UNSUPPORTED).build();
                }

                syncDoneWithMessage(blueprintLibrary,
                        MessageFormat.format("Skipping update for blueprint library {0} because current beaver version {1} is below minimum required version {2}",
                        blueprintLibraryAddress.getKey(),
                        currentBeaverVersion,
                        manifest.getMinimumRequiredBeaverIotVersion()));
                switchDefaultBlueprintLibrarySubscriptionForAllTenantsIfNeeded(blueprintLibrary, blueprintLibraryAddress);
                return blueprintLibrary;
            }

            doSync(blueprintLibrary, manifest, blueprintLibraryAddress, start);
            return blueprintLibrary;
        } catch (Exception e) {
            log.error("Sync blueprint library {} failed", blueprintLibraryAddress.getKey(), e);
            blueprintLibrary.setSyncStatus(BlueprintLibrarySyncStatus.SYNC_FAILED);
            blueprintLibrary.setSyncedAt(System.currentTimeMillis());
            blueprintLibrary.setSyncMessage(MessageFormat.format("Sync failed. Error Message: {0}", e.getMessage()));
            blueprintLibraryService.save(blueprintLibrary);
            throw e;
        }
    }

    public void syncDoneWithMessage(BlueprintLibrary blueprintLibrary, String syncMessage) {
        blueprintLibrary.setSyncStatus(BlueprintLibrarySyncStatus.SYNCED);
        blueprintLibrary.setSyncMessage(syncMessage);
        blueprintLibrary.setSyncedAt(System.currentTimeMillis());
        blueprintLibraryService.save(blueprintLibrary);
        log.debug(syncMessage);
    }

    public void addListener(Consumer<BlueprintLibrary> listener) {
        listeners.add(listener);
    }

    private void doSync(BlueprintLibrary blueprintLibrary, BlueprintLibraryManifest manifest, BlueprintLibraryAddress blueprintLibraryAddress, long start) throws Exception {
        log.debug("Start syncing blueprint library: {}", blueprintLibraryAddress.getKey());

        blueprintLibrary.setRemoteVersion(manifest.getVersion());
        blueprintLibrary.setSyncMessage("Start syncing blueprint library");
        blueprintLibraryService.save(blueprintLibrary);

        String codeZipUrl = blueprintLibraryAddress.getCodeZipUrl();
        if (StringUtils.isEmpty(codeZipUrl)) {
            throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_ZIP_URL_EMPTY).build();
        }

        List<BlueprintLibraryResource> blueprintLibraryResources = new ArrayList<>();
        try {
            if (blueprintLibrary.getSourceType() == BlueprintLibrarySourceType.Upload) {
                try (InputStream inputStream = resourceManagerFacade.getDataByUrl(codeZipUrl)) {
                    syncBlueprintLibraryResources(inputStream, blueprintLibrary, manifest, blueprintLibraryAddress, blueprintLibraryResources);
                }
            } else {
                Request request = new Request.Builder().url(codeZipUrl).build();
                try (Response response = OkHttpUtil.getClient().newCall(request).execute();
                     InputStream inputStream = response.body() != null ? response.body().byteStream() : null) {
                    if (!response.isSuccessful()) {
                        throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_RESOURCES_FETCH_FAILED).build();
                    }

                    syncBlueprintLibraryResources(inputStream, blueprintLibrary, manifest, blueprintLibraryAddress, blueprintLibraryResources);
                }
            }

            notifyListeners(blueprintLibrary);
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }

        log.debug("Fishing syncing blueprint library: {}, time: {} ms", blueprintLibraryAddress.getKey(), System.currentTimeMillis() - start);
    }

    private void syncBlueprintLibraryResources(InputStream inputStream, BlueprintLibrary blueprintLibrary,
                                               BlueprintLibraryManifest manifest,
                                               BlueprintLibraryAddress blueprintLibraryAddress,
                                               List<BlueprintLibraryResource> blueprintLibraryResources) throws Exception {
        if (inputStream == null) {
            throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_RESOURCES_FETCH_FAILED).build();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry rootEntry = zipInputStream.getNextEntry();
            if (rootEntry == null) {
                throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_RESOURCES_FETCH_FAILED).build();
            }

            String rootPrefix = rootEntry.getName();
            if (!rootEntry.isDirectory() || !rootPrefix.endsWith("/")) {
                throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_RESOURCES_FETCH_FAILED).build();
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
            throw ServiceException.with(BlueprintLibraryErrorCode.BLUEPRINT_LIBRARY_RESOURCES_FETCH_FAILED).build();
        }

        blueprintLibraryResourceService.deleteAllByLibraryIdAndLibraryVersion(blueprintLibrary.getId(), manifest.getVersion());
        blueprintLibraryResourceService.batchSave(blueprintLibraryResources);

        BlueprintLibrary oldBlueprintLibrary = BlueprintLibrary.clone(blueprintLibrary);
        blueprintLibrary.setCurrentVersion(manifest.getVersion());
        syncDoneWithMessage(blueprintLibrary, "Synced blueprint library successfully");
        saveBlueprintLibraryVersion(blueprintLibrary);
        updateBlueprintLibrarySubscriptionsForAllTenants(blueprintLibrary, blueprintLibraryAddress);

        tenantFacade.runWithAllTenants(() -> evictCaches(oldBlueprintLibrary));
    }

    private void saveBlueprintLibraryVersion(BlueprintLibrary blueprintLibrary) {
        // Create new version for blueprint library
        Long libraryId = blueprintLibrary.getId();
        String libraryVersion = blueprintLibrary.getCurrentVersion();
        BlueprintLibraryVersion blueprintLibraryVersion = blueprintLibraryVersionService.findByLibraryIdAndLibraryVersion(libraryId, libraryVersion);
        if (blueprintLibraryVersion == null) {
            blueprintLibraryVersion = BlueprintLibraryVersion.builder()
                    .libraryId(libraryId)
                    .libraryVersion(libraryVersion)
                    .syncedAt(System.currentTimeMillis())
                    .build();
            blueprintLibraryVersionService.save(blueprintLibraryVersion);
        }
    }

    private void updateBlueprintLibrarySubscriptionsForAllTenants(BlueprintLibrary blueprintLibrary, BlueprintLibraryAddress blueprintLibraryAddress) {
        // Update blueprint library subscriptions for all tenants
        Long libraryId = blueprintLibrary.getId();
        String libraryVersion = blueprintLibrary.getCurrentVersion();
        tenantFacade.runWithAllTenants(() -> {
            List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions = blueprintLibrarySubscriptionService.findAll();
            BlueprintLibrarySubscription blueprintLibrarySubscription = filterBlueprintLibrarySubscription(blueprintLibrarySubscriptions, libraryId);
            BlueprintLibrarySubscription activeBlueprintLibrarySubscription = filterActiveBlueprintLibrarySubscription(blueprintLibrarySubscriptions);

            boolean isDefaultBlueprintLibraryAddress = blueprintLibraryAddressService.isDefaultBlueprintLibraryAddress(blueprintLibraryAddress);
            if (isDefaultBlueprintLibraryAddress) {
                if (blueprintLibrarySubscription == null) {
                    blueprintLibrarySubscription = BlueprintLibrarySubscription.builder()
                            .libraryId(libraryId)
                            .libraryVersion(libraryVersion)
                            .active(false)
                            .build();
                }
                blueprintLibrarySubscription.setLibraryVersion(libraryVersion);

                if (activeBlueprintLibrarySubscription == null) {
                    // Set default blueprint library active
                    blueprintLibrarySubscriptionService.save(blueprintLibrarySubscription);
                    blueprintLibrarySubscriptionService.setActiveOnlyByLibraryId(libraryId);
                } else {
                    BlueprintLibrary activeBlueprintLibrary = blueprintLibraryService.findById(activeBlueprintLibrarySubscription.getLibraryId());
                    if (!activeBlueprintLibrarySubscription.getLibraryId().equals(libraryId) && activeBlueprintLibrary.getSourceType() == BlueprintLibrarySourceType.Default) {
                        // Switch the latest default blueprint library active
                        blueprintLibrarySubscriptionService.save(blueprintLibrarySubscription);
                        blueprintLibrarySubscriptionService.setActiveOnlyByLibraryId(libraryId);
                    } else {
                        blueprintLibrarySubscriptionService.save(blueprintLibrarySubscription);
                    }
                }
            } else {
                if (blueprintLibrarySubscription == null) {
                    return;
                }

                blueprintLibrarySubscription.setLibraryVersion(libraryVersion);
                blueprintLibrarySubscriptionService.save(blueprintLibrarySubscription);
            }
        });
    }

    private BlueprintLibrarySubscription filterBlueprintLibrarySubscription(List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions, Long libraryId) {
        return blueprintLibrarySubscriptions
                .stream()
                .filter(subscription -> subscription.getLibraryId().equals(libraryId))
                .findFirst()
                .orElse(null);
    }

    private BlueprintLibrarySubscription filterActiveBlueprintLibrarySubscription(List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions) {
        return blueprintLibrarySubscriptions
                .stream()
                .filter(BlueprintLibrarySubscription::getActive)
                .findFirst()
                .orElse(null);
    }

    private void switchDefaultBlueprintLibrarySubscriptionForAllTenantsIfNeeded(BlueprintLibrary blueprintLibrary, BlueprintLibraryAddress blueprintLibraryAddress) {
        // Switch blueprint library subscriptions for all tenants if needed
        boolean isDefaultBlueprintLibraryAddress = blueprintLibraryAddressService.isDefaultBlueprintLibraryAddress(blueprintLibraryAddress);
        if (isDefaultBlueprintLibraryAddress) {
            Long libraryId = blueprintLibrary.getId();

            tenantFacade.runWithAllTenants(() -> {
                List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions = blueprintLibrarySubscriptionService.findAll();
                BlueprintLibrarySubscription activeBlueprintLibrarySubscription = filterActiveBlueprintLibrarySubscription(blueprintLibrarySubscriptions);

                assert activeBlueprintLibrarySubscription != null;
                BlueprintLibrary activeBlueprintLibrary = blueprintLibraryService.findById(activeBlueprintLibrarySubscription.getLibraryId());
                if (!activeBlueprintLibrarySubscription.getLibraryId().equals(libraryId) && activeBlueprintLibrary.getSourceType() == BlueprintLibrarySourceType.Default) {
                    // Switch the latest default blueprint library active
                    blueprintLibrarySubscriptionService.setActiveOnlyByLibraryId(libraryId);
                }
            });
        }
    }

    private void notifyListeners(BlueprintLibrary blueprintLibrary) {
        List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions = blueprintLibrarySubscriptionService.findAllByActiveTrueIgnoreTenant();
        if (CollectionUtils.isEmpty(blueprintLibrarySubscriptions)) {
            return;
        }

        for (BlueprintLibrarySubscription blueprintLibrarySubscription : blueprintLibrarySubscriptions) {
            if (blueprintLibrarySubscription.getLibraryId().equals(blueprintLibrary.getId())) {
                String tenantId = blueprintLibrarySubscription.getTenantId();
                notifyListenersWithTenant(blueprintLibrary, tenantId);
            }
        }
    }

    public void notifyListenersWithTenant(BlueprintLibrary blueprintLibrary, String tenantId) {
        BlueprintLibrary finalBlueprintLibrary = BlueprintLibrary.clone(blueprintLibrary);
        listeners.forEach(listener -> CompletableFuture.runAsync(() -> {
            try {
                TenantContext.setTenantId(tenantId);
                listener.accept(finalBlueprintLibrary);
            } catch (Exception e) {
                log.error("Listener failed", e);
            }
        }, listenerExecutor));
    }

    @PreDestroy
    public void destroy() {
        listenerExecutor.shutdown();
        try {
            if (!listenerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                listenerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            listenerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void evictCaches(BlueprintLibrary oldBlueprintLibrary) {
        if (oldBlueprintLibrary.getCurrentVersion() == null) {
            return;
        }

        List<BlueprintDeviceVendor> deviceVendors = blueprintLibraryResourceResolver.getDeviceVendors();
        blueprintLibraryService.evictCacheBlueprintLibrary(oldBlueprintLibrary.getType().name(), oldBlueprintLibrary.getUrl(), oldBlueprintLibrary.getBranch());
        blueprintLibraryResourceResolver.evictCacheDeviceVendors(oldBlueprintLibrary);
        if (!CollectionUtils.isEmpty(deviceVendors)) {
            deviceVendors.forEach(vendor -> blueprintLibraryResourceResolver.evictCacheDeviceModels(oldBlueprintLibrary, vendor.getId()));
        }
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