package com.milesight.beaveriot.device.status.local;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.device.status.AbstractDeviceStatusManager;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/7/21 14:46
 **/
@SuppressWarnings("unused")
public class DeviceStatusLocalManager extends AbstractDeviceStatusManager implements DeviceStatusManager {
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> deviceTimerFutures = new ConcurrentHashMap<>();

    public DeviceStatusLocalManager(DeviceServiceProvider deviceServiceProvider, EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider) {
        super(deviceServiceProvider, entityServiceProvider, entityValueServiceProvider);
        this.scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {
        deviceTimerFutures.values().forEach(future -> future.cancel(true));
        scheduler.shutdown();
    }

    @Override
    protected void afterRegister(String integrationId, DeviceStatusConfig config) {
        List<Device> devices = deviceServiceProvider.findAll(integrationId);
        if (config != null && !CollectionUtils.isEmpty(devices)) {
            Function<Device, Long> offlineSecondsFetcher = config.getOfflineSecondsFetcher();
            devices.forEach(device -> {
                long offlineSeconds = Optional.ofNullable(offlineSecondsFetcher)
                        .map(f -> f.apply(device))
                        .orElse(DEFAULT_OFFLINE_SECONDS);
                startOfflineCountdown(device, offlineSeconds);
            });
        }
    }

    @Override
    public void dataUploaded(Device device, ExchangePayload payload) {
        cancelOfflineCountdown(device);
        DeviceStatusConfig config = integrationDeviceStatusConfigs.get(device.getIntegrationId());
        config.getOnlineUpdater().accept(device, payload);
        long offlineSeconds = Optional.ofNullable(config.getOfflineSecondsFetcher())
                .map(f -> f.apply(device))
                .orElse(DEFAULT_OFFLINE_SECONDS);
        startOfflineCountdown(device, offlineSeconds);
    }

    private void startOfflineCountdown(Device device, long offlineSeconds) {
        if (deviceTimerFutures.containsKey(device.getKey())) {
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() ->
                doUpdateDeviceStatusToOffline(device.getKey())
                , offlineSeconds, TimeUnit.SECONDS);
        deviceTimerFutures.put(device.getKey(), future);
    }

    private void doUpdateDeviceStatusToOffline(String deviceKey) {
        deviceTimerFutures.remove(deviceKey);

        updateDeviceStatusToOfflineByDeviceKey(deviceKey);
    }

    private void cancelOfflineCountdown(Device device) {
        ScheduledFuture<?> future = deviceTimerFutures.get(device.getKey());
        if (future != null) {
            future.cancel(false);
            deviceTimerFutures.remove(device.getKey());
        }
    }

    @Override
    public void deviceDeleted(Device device) {
        cancelOfflineCountdown(device);
    }
}
