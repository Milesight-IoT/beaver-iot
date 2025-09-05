package com.milesight.beaveriot.device.status.local;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.device.status.BaseDeviceStatusManager;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * author: Luxb
 * create: 2025/7/21 14:46
 **/
@SuppressWarnings("unused")
public class DeviceStatusLocalManager extends BaseDeviceStatusManager implements DeviceStatusManager {
    private final ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> deviceTimerFutures = new ConcurrentHashMap<>();

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
            devices.forEach(device -> {
                long offlineSeconds = getDeviceOfflineSeconds(device, config);
                startOfflineCountdown(device, offlineSeconds);
            });
        }
    }

    @Override
    public void dataUploaded(Device device, ExchangePayload payload) {
        cancelOfflineCountdown(device);

        AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDeviceId(device.getId());
        if (availableDeviceData == null) {
            return;
        }

        DeviceStatusConfig config = availableDeviceData.getDeviceStatusConfig();
        config.getOnlineUpdater().accept(device, payload);

        long offlineSeconds = getDeviceOfflineSeconds(device, config);
        startOfflineCountdown(device, offlineSeconds);
        deviceOnlineCallback(device, System.currentTimeMillis() / 1000 + offlineSeconds);
    }

    private void startOfflineCountdown(Device device, long offlineSeconds) {
        if (deviceTimerFutures.containsKey(device.getId())) {
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() ->
                doUpdateDeviceStatusToOffline(device)
                , offlineSeconds, TimeUnit.SECONDS);
        deviceTimerFutures.put(device.getId(), future);
    }

    private void doUpdateDeviceStatusToOffline(Device device) {
        deviceTimerFutures.remove(device.getId());

        AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDeviceId(device.getId());
        if (availableDeviceData == null) {
            return;
        }

        availableDeviceData.getDeviceStatusConfig().getOfflineUpdater().accept(device);
        deviceOfflineCallback(device);
    }

    private void cancelOfflineCountdown(Device device) {
        ScheduledFuture<?> future = deviceTimerFutures.get(device.getId());
        if (future != null) {
            future.cancel(false);
            deviceTimerFutures.remove(device.getId());
        }
    }

    @Override
    public void deviceDeleted(Device device) {
        cancelOfflineCountdown(device);
    }
}
