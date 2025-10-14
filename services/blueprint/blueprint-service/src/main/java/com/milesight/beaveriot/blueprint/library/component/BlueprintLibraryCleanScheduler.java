package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.blueprint.library.config.BlueprintLibraryConfig;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibrarySubscription;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryVersion;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryAddressService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibrarySubscriptionService;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryVersionService;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateFacade;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/9/30 8:55
 **/
@Slf4j
@Service
@Order(2)
public class BlueprintLibraryCleanScheduler implements CommandLineRunner {
    private final BlueprintLibraryConfig blueprintLibraryConfig;
    private final Scheduler scheduler;
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;
    private final IDeviceTemplateFacade deviceTemplateFacade;
    private final IDeviceFacade deviceFacade;
    private final BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService;
    private final BlueprintLibraryVersionService blueprintLibraryVersionService;
    private final BlueprintLibraryService blueprintLibraryService;
    private final BlueprintLibraryCleaner blueprintLibraryCleaner;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public BlueprintLibraryCleanScheduler(BlueprintLibraryConfig blueprintLibraryConfig, Scheduler scheduler, BlueprintLibraryAddressService blueprintLibraryAddressService, IDeviceTemplateFacade deviceTemplateFacade, IDeviceFacade deviceFacade, BlueprintLibrarySubscriptionService blueprintLibrarySubscriptionService, BlueprintLibraryVersionService blueprintLibraryVersionService, BlueprintLibraryService blueprintLibraryService, BlueprintLibraryCleaner blueprintLibraryCleaner) {
        this.blueprintLibraryConfig = blueprintLibraryConfig;
        this.scheduler = scheduler;
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.deviceTemplateFacade = deviceTemplateFacade;
        this.deviceFacade = deviceFacade;
        this.blueprintLibrarySubscriptionService = blueprintLibrarySubscriptionService;
        this.blueprintLibraryVersionService = blueprintLibraryVersionService;
        this.blueprintLibraryService = blueprintLibraryService;
        this.blueprintLibraryCleaner = blueprintLibraryCleaner;
    }

    @Override
    public void run(String... args) {
        // Start blueprint library clean scheduled task
        start();
    }

    public void start() {
        cleanBlueprintLibraries();

        ScheduleRule rule = new ScheduleRule();
        rule.setPeriodSecond(blueprintLibraryConfig.getCleanFrequency().toSeconds());
        ScheduleSettings settings = new ScheduleSettings();
        settings.setScheduleType(ScheduleType.FIXED_RATE);
        settings.setScheduleRule(rule);
        scheduler.schedule("clean-blueprint-library", settings, task -> this.cleanBlueprintLibraries());
    }

    protected void cleanBlueprintLibraries() {
        long start = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        try {
            List<BlueprintLibraryVersion> unusedBlueprintLibraryVersions = getUnusedBlueprintLibraryVersions();
            if (CollectionUtils.isEmpty(unusedBlueprintLibraryVersions)) {
                log.debug("Skipping clean blueprint library versions because all are in use");
                return;
            }

            log.info("Start cleaning blueprint library versions, total: {}", unusedBlueprintLibraryVersions.size());

            Map<Long, List<BlueprintLibraryVersion>> unusedBlueprintLibraryVersionsMap = unusedBlueprintLibraryVersions.stream()
                    .collect(Collectors.groupingBy(BlueprintLibraryVersion::getLibraryId));
            unusedBlueprintLibraryVersionsMap.forEach((libraryId, eachUnusedBlueprintLibraryVersions) -> {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                eachUnusedBlueprintLibraryVersions.forEach(unusedBlueprintLibraryVersion -> {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            blueprintLibraryCleaner.clean(unusedBlueprintLibraryVersion);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            log.error("Error occurred while cleaning blueprint library version (libraryId:{}, libraryVersion:{})",
                                    unusedBlueprintLibraryVersion.getLibraryId(),
                                    unusedBlueprintLibraryVersion.getLibraryVersion(), e);
                            failedCount.incrementAndGet();
                        }
                    }, executor);
                    futures.add(future);
                });
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Set the current version to the latest version for each blueprint library
                BlueprintLibrary blueprintLibrary = blueprintLibraryService.findById(libraryId);
                if (blueprintLibrary == null) {
                    return;
                }

                List<BlueprintLibraryVersion> blueprintLibraryVersions = blueprintLibraryVersionService.findAllByLibraryId(libraryId)
                        .stream()
                        .sorted(Comparator.comparing(BlueprintLibraryVersion::getLibraryVersion, this::compareVersion).reversed())
                        .toList();
                String latestVersion = blueprintLibraryVersions.get(0).getLibraryVersion();
                if (blueprintLibrary.getCurrentVersion().equals(latestVersion)) {
                    return;
                }

                blueprintLibrary.setCurrentVersion(latestVersion);
                blueprintLibraryService.save(blueprintLibrary);
                blueprintLibraryService.evictCacheBlueprintLibrary(blueprintLibrary.getType().name(), blueprintLibrary.getUrl(), blueprintLibrary.getBranch());
            });
        } catch (Exception e) {
            log.error("Error occurred while scheduling clean for blueprint library versions", e);
        }
        log.info("Finish cleaning blueprint library versions, success: {}, failed: {}, time: {} ms",
                successCount.get(), failedCount.get(), System.currentTimeMillis() - start);
    }

    private int compareVersion(String version, String otherVersion) {
        String[] versionParts = version.split("\\.");
        int versionMajor = Integer.parseInt(versionParts[0]);
        int versionMinor = Integer.parseInt(versionParts[1]);

        String[] otherVersionParts = otherVersion.split("\\.");
        int otherVersionMajor = Integer.parseInt(otherVersionParts[0]);
        int otherVersionMinor = Integer.parseInt(otherVersionParts[1]);

        if (versionMajor != otherVersionMajor) {
            return Integer.compare(versionMajor, otherVersionMajor);
        }

        return Integer.compare(versionMinor, otherVersionMinor);
    }

    private List<BlueprintLibraryVersion> getUnusedBlueprintLibraryVersions() {
        List<BlueprintLibraryVersion> allBlueprintLibraryVersions = blueprintLibraryVersionService.findAll();
        if (CollectionUtils.isEmpty(allBlueprintLibraryVersions)) {
            return null;
        }

        Map<Long, BlueprintLibrary> blueprintLibraryMap = new HashMap<>();
        Map<Long, List<BlueprintLibraryVersion>> blueprintLibraryVersionMap = new TreeMap<>();
        for (BlueprintLibraryVersion blueprintLibraryVersion : allBlueprintLibraryVersions) {
            Long libraryId = blueprintLibraryVersion.getLibraryId();
            List<BlueprintLibraryVersion> blueprintLibraryVersions = blueprintLibraryVersionMap.computeIfAbsent(libraryId, k -> new ArrayList<>());
            blueprintLibraryVersions.add(blueprintLibraryVersion);
            blueprintLibraryMap.putIfAbsent(libraryId, blueprintLibraryService.findById(libraryId));
        }

        Set<String> activeBlueprintLibrarySubscriptionKeys = new HashSet<>();
        List<BlueprintLibrarySubscription> blueprintLibrarySubscriptions = blueprintLibrarySubscriptionService.findAllIgnoreTenant();
        if (!CollectionUtils.isEmpty(blueprintLibrarySubscriptions)) {
            for (BlueprintLibrarySubscription blueprintLibrarySubscription : blueprintLibrarySubscriptions) {
                String key = blueprintLibrarySubscription.getKey();
                if (blueprintLibrarySubscription.getActive()) {
                    activeBlueprintLibrarySubscriptionKeys.add(key);
                }
            }
        }

        List<BlueprintLibraryVersion> inactiveBlueprintLibraryVersions = new ArrayList<>();
        for (Long libraryId : blueprintLibraryVersionMap.keySet()) {
            BlueprintLibrary blueprintLibrary = blueprintLibraryMap.get(libraryId);
            BlueprintLibraryAddress blueprintLibraryAddress = blueprintLibraryAddressService.convertLibraryToAddress(blueprintLibrary);
            if (blueprintLibraryAddressService.isDefaultBlueprintLibraryAddress(blueprintLibraryAddress)) {
                continue;
            }

            List<BlueprintLibraryVersion> blueprintLibraryVersions = blueprintLibraryVersionMap.get(libraryId);
            for (BlueprintLibraryVersion blueprintLibraryVersion : blueprintLibraryVersions) {
                if (!activeBlueprintLibrarySubscriptionKeys.contains(blueprintLibraryVersion.getKey())) {
                    inactiveBlueprintLibraryVersions.add(blueprintLibraryVersion);
                }
            }
        }

        if (CollectionUtils.isEmpty(inactiveBlueprintLibraryVersions)) {
            return null;
        }

        List<BlueprintLibraryVersion> unusedBlueprintLibraryVersions = new ArrayList<>();
        for (BlueprintLibraryVersion inactiveBlueprintLibraryVersion : inactiveBlueprintLibraryVersions) {
            List<DeviceTemplate> deviceTemplates = deviceTemplateFacade.findByBlueprintLibraryIgnoreTenant(inactiveBlueprintLibraryVersion.getLibraryId(), inactiveBlueprintLibraryVersion.getLibraryVersion());
            if (CollectionUtils.isEmpty(deviceTemplates)) {
                unusedBlueprintLibraryVersions.add(inactiveBlueprintLibraryVersion);
                continue;
            }

            List<String> deviceTemplateKeys = deviceTemplates.stream().map(DeviceTemplate::getKey).distinct().toList();
            long deviceCount = deviceFacade.countByTemplateInIgnoreTenant(deviceTemplateKeys);
            if (deviceCount == 0) {
                unusedBlueprintLibraryVersions.add(inactiveBlueprintLibraryVersion);
            }
        }
        return unusedBlueprintLibraryVersions;
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
