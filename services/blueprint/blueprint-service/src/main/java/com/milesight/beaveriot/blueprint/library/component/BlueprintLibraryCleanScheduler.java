package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.blueprint.library.config.BlueprintLibraryConfig;
import com.milesight.beaveriot.blueprint.library.service.BlueprintLibraryAddressService;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * author: Luxb
 * create: 2025/9/30 8:55
 **/
@Slf4j
@Service
@Order(1)
public class BlueprintLibraryCleanScheduler implements CommandLineRunner {
    private final BlueprintLibraryConfig blueprintLibraryConfig;
    private final Scheduler scheduler;
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;

    public BlueprintLibraryCleanScheduler(BlueprintLibraryConfig blueprintLibraryConfig, Scheduler scheduler, BlueprintLibraryAddressService blueprintLibraryAddressService) {
        this.blueprintLibraryConfig = blueprintLibraryConfig;
        this.scheduler = scheduler;
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
    }

    @Override
    public void run(String... args) throws Exception {
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

    }
}
