package com.milesight.beaveriot.resource.manager.constants;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * ResourceConstants class.
 *
 * @author simon
 * @date 2025/4/14
 */
public class ResourceManagerConstants {
    private ResourceManagerConstants() {}

    public static final int MAX_TEMP_RESOURCE_LIVE_MINUTES = 360;

    public static final int TEMP_RESOURCE_LIVE_MINUTES = 15;

    public static final Duration CLEAR_TEMP_RESOURCE_INTERVAL = Duration.of(1, ChronoUnit.MINUTES);

    public static final Duration AUTO_UNLINK_RESOURCE_INTERVAL = Duration.of(1, ChronoUnit.MINUTES);

    public static final int CLEAR_TEMP_BATCH_SIZE = 1_000;
}
