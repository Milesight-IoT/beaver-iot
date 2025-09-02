package com.milesight.beaveriot.blueprint.component;

import com.milesight.beaveriot.blueprint.config.BlueprintRepositoryConfig;
import com.milesight.beaveriot.blueprint.model.BlueprintRepositoryAddress;
import com.milesight.beaveriot.blueprint.service.BlueprintRepositoryAddressService;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
@Component
public class BlueprintRepositorySyncScheduler {
    private final BlueprintRepositoryConfig blueprintRepositoryConfig;
    private final Scheduler scheduler;
    private final BlueprintRepositoryAddressService blueprintRepositoryAddressService;
    private final BlueprintRepositorySyncer blueprintRepositorySyncer;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public BlueprintRepositorySyncScheduler(BlueprintRepositoryConfig blueprintRepositoryConfig, Scheduler scheduler, BlueprintRepositoryAddressService blueprintRepositoryAddressService, BlueprintRepositorySyncer blueprintRepositorySyncer) {
        this.blueprintRepositoryConfig = blueprintRepositoryConfig;
        this.scheduler = scheduler;
        this.blueprintRepositoryAddressService = blueprintRepositoryAddressService;
        this.blueprintRepositorySyncer = blueprintRepositorySyncer;
    }

    @PostConstruct
    public void start() {
        log.info("------------------------------start scheduler---------------------------");
        ScheduleRule rule = new ScheduleRule();
        rule.setPeriodSecond(blueprintRepositoryConfig.getSyncFrequency().toSeconds());
        ScheduleSettings settings = new ScheduleSettings();
        settings.setScheduleType(ScheduleType.FIXED_RATE);
        settings.setScheduleRule(rule);
        scheduler.schedule("sync-blueprint-repository", settings, task -> this.syncBlueprintRepositories());
    }

    protected void syncBlueprintRepositories() {
        long start = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        try {
            List<BlueprintRepositoryAddress> blueprintRepositoryAddresses = blueprintRepositoryAddressService.getDistinctBlueprintRepositoryAddresses();
            if (CollectionUtils.isEmpty(blueprintRepositoryAddresses)) {
                return;
            }

            log.info("Start syncing blueprint repositories, total: {}", blueprintRepositoryAddresses.size());

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            blueprintRepositoryAddresses.forEach(blueprintRepositoryAddress -> {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        blueprintRepositorySyncer.sync(blueprintRepositoryAddress);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Error occurred while syncing blueprint repository {}", blueprintRepositoryAddress.getKey(), e);
                        failedCount.incrementAndGet();
                    }
                }, executor);
                futures.add(future);
            });
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Error occurred while scheduling sync for blueprint repositories", e);
        }
        log.info("Finish syncing blueprint repositories, success: {}, failed: {}, time: {} ms",
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
