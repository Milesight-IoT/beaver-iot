package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.blueprint.library.config.BlueprintLibraryConfig;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryAddressService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author: Luxb
 * create: 2025/9/1 9:56
 **/
@Slf4j
@Service
@Order(1)
public class BlueprintLibrarySyncScheduler implements CommandLineRunner {
    private final BlueprintLibraryConfig blueprintLibraryConfig;
    private final Scheduler scheduler;
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;
    private final BlueprintLibrarySyncer blueprintLibrarySyncer;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public BlueprintLibrarySyncScheduler(BlueprintLibraryConfig blueprintLibraryConfig, Scheduler scheduler, BlueprintLibraryAddressService blueprintLibraryAddressService, BlueprintLibrarySyncer blueprintLibrarySyncer) {
        this.blueprintLibraryConfig = blueprintLibraryConfig;
        this.scheduler = scheduler;
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
        this.blueprintLibrarySyncer = blueprintLibrarySyncer;
    }

    @Override
    public void run(String... args) {
        // Start blueprint library synchronization scheduled task
        start();
    }

    public void start() {
        // Synchronize the blueprint library once immediately when the application starts.
        syncBlueprintLibraries();

        ScheduleRule rule = new ScheduleRule();
        rule.setPeriodSecond(blueprintLibraryConfig.getSyncFrequency().toSeconds());
        ScheduleSettings settings = new ScheduleSettings();
        settings.setScheduleType(ScheduleType.FIXED_RATE);
        settings.setScheduleRule(rule);
        scheduler.schedule("sync-blueprint-library", settings, task -> this.syncBlueprintLibraries());
    }

    protected void syncBlueprintLibraries() {
        long start = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        try {
            List<BlueprintLibraryAddress> blueprintLibraryAddresses = blueprintLibraryAddressService.getDistinctBlueprintLibraryAddresses();
            if (CollectionUtils.isEmpty(blueprintLibraryAddresses)) {
                return;
            }

            log.info("Start syncing blueprint libraries, total: {}", blueprintLibraryAddresses.size());

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            blueprintLibraryAddresses.forEach(blueprintLibraryAddress -> {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        blueprintLibrarySyncer.sync(blueprintLibraryAddress);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Error occurred while syncing blueprint library {}", blueprintLibraryAddress.getKey(), e);
                        failedCount.incrementAndGet();
                    }
                }, executor);
                futures.add(future);
            });
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Error occurred while scheduling sync for blueprint libraries", e);
        }
        log.info("Finish syncing blueprint libraries, success: {}, failed: {}, time: {} ms",
                successCount.get(), failedCount.get(), System.currentTimeMillis() - start);
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
