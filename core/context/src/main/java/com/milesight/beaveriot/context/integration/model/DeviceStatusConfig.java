package com.milesight.beaveriot.context.integration.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/29 13:03
 **/
@Builder
@Data
public class DeviceStatusConfig {
    private Function<Device, Long> offlineTimeoutFetcher;
    private Function<List<Device>, Map<Long, Long>> batchOfflineTimeoutFetcher;
    private Consumer<Device> onlineListener;
    private Consumer<Device> offlineListener;
}
