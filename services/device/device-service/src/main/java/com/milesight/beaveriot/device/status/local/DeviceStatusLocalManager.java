package com.milesight.beaveriot.device.status.local;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatusConfig;
import com.milesight.beaveriot.device.status.BaseDeviceStatusManager;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import org.springframework.aop.framework.AopContext;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/7/21 14:46
 **/
@SuppressWarnings("unused")
public class DeviceStatusLocalManager extends BaseDeviceStatusManager implements DeviceStatusManager {
    private static final int BATCH_QUERY_OFFLINE_TIMEOUT_SIZE = 1000;
    private final ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> deviceTimerFutures = new ConcurrentHashMap<>();
    private final Map<Long, Long> deviceExpirationTimeMap;

    public DeviceStatusLocalManager(DeviceServiceProvider deviceServiceProvider,
                                    EntityServiceProvider entityServiceProvider,
                                    EntityValueServiceProvider entityValueServiceProvider,
                                    EntityTemplateServiceProvider entityTemplateServiceProvider) {
        super(deviceServiceProvider, entityServiceProvider, entityValueServiceProvider, entityTemplateServiceProvider);
        this.scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        this.deviceExpirationTimeMap = new ConcurrentHashMap<>();
    }

    @Override
    protected void onInit() {

    }

    @Override
    protected void onDestroy() {
        deviceTimerFutures.values().forEach(future -> future.cancel(true));
        scheduler.shutdown();
    }

    @Override
    protected void afterRegister(String integrationId, DeviceStatusConfig config) {
        List<Device> devices = deviceServiceProvider.findAll(integrationId);
        if (config != null && !CollectionUtils.isEmpty(devices)) {
            initDevices(devices, config);
        }
    }

    private void initDevices(List<Device> devices, DeviceStatusConfig config) {
        Function<List<Device>, Map<Long, Duration>> batchOfflineTimeoutFetcher = config.getBatchOfflineTimeoutFetcher();
        if (batchOfflineTimeoutFetcher == null) {
            devices.forEach(device -> {
                Duration offlineDuration = getDeviceOfflineDuration(device, config);
                if (offlineDuration != null) {
                    startOfflineCountdown(device, offlineDuration);
                }
            });
        } else {
            int totalSize = devices.size();
            for (int i = 0; i < totalSize; i += BATCH_QUERY_OFFLINE_TIMEOUT_SIZE) {
                int endIndex = Math.min(i + BATCH_QUERY_OFFLINE_TIMEOUT_SIZE, totalSize);
                List<Device> batchDevices = devices.subList(i, endIndex);
                Map<Long, Duration> deviceOfflineTimeoutMap = batchOfflineTimeoutFetcher.apply(batchDevices);
                if (CollectionUtils.isEmpty(deviceOfflineTimeoutMap)) {
                    continue;
                }

                batchDevices.forEach(device -> {
                    Duration offlineDuration = deviceOfflineTimeoutMap.get(device.getId());
                    if (offlineDuration != null) {
                        startOfflineCountdown(device, offlineDuration);
                    }
                });
            }
        }
    }

    @Override
    public void online(Device device) {
        AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDeviceId(device.getId());
        if (availableDeviceData == null) {
            updateDeviceStatusToOnline(device, null);
            return;
        }

        self().handleStatus(device.getId(), availableDeviceData, DeviceStatusOperation.ONLINE);
    }

    private DeviceStatusLocalManager self() {
        return (DeviceStatusLocalManager) AopContext.currentProxy();
    }

    @DistributedLock(name = "device:status:handle:#{#p0}", waitForLock = "5s", throwOnLockFailure = false)
    public void handleStatus(Long deviceId,
                             AvailableDeviceData availableDeviceData,
                             DeviceStatusOperation operation) {
        if (availableDeviceData == null) {
            deviceTimerFutures.remove(deviceId);
            return;
        }

        if (operation == DeviceStatusOperation.ONLINE) {
            handleStatusToOnline(availableDeviceData);
        } else {
            handleStatusToOffline(availableDeviceData);
        }
    }

    private void handleStatusToOnline(AvailableDeviceData availableDeviceData) {
        Device device = availableDeviceData.getDevice();
        cancelOfflineCountdown(device);

        DeviceStatusConfig config = availableDeviceData.getDeviceStatusConfig();
        updateDeviceStatusToOnline(device, config.getOnlineListener());

        Duration offlineDuration = getDeviceOfflineDuration(device, config);
        if (offlineDuration != null) {
            Long expirationTime = System.currentTimeMillis() + offlineDuration.toSeconds() * 1000;
            deviceExpirationTimeMap.put(device.getId(), expirationTime);
            startOfflineCountdown(device, offlineDuration);
        }
    }

    private void handleStatusToOffline(AvailableDeviceData availableDeviceData) {
        Device device = availableDeviceData.getDevice();
        DeviceStatusConfig config = availableDeviceData.getDeviceStatusConfig();
        Long expirationTime = deviceExpirationTimeMap.get(device.getId());
        if (expirationTime != null && System.currentTimeMillis() < expirationTime) {
            return;
        }

        deviceExpirationTimeMap.remove(device.getId());
        deviceTimerFutures.remove(device.getId());

        updateDeviceStatusToOffline(device, config.getOfflineListener());
    }

    private void startOfflineCountdown(Device device, Duration offlineDuration) {
        if (deviceTimerFutures.containsKey(device.getId())) {
            return;
        }

        DeviceStatusLocalManager self = self();
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDeviceId(device.getId());
            self.handleStatus(device.getId(), availableDeviceData, DeviceStatusOperation.OFFLINE);
        }, offlineDuration.toSeconds(), TimeUnit.SECONDS);
        deviceTimerFutures.put(device.getId(), future);
    }

    private void cancelOfflineCountdown(Device device) {
        ScheduledFuture<?> future = deviceTimerFutures.get(device.getId());
        if (future != null) {
            future.cancel(false);
            deviceTimerFutures.remove(device.getId());
        }
    }

    @Override
    public void deregister(Device device) {
        deviceExpirationTimeMap.remove(device.getId());

        cancelOfflineCountdown(device);
    }
}
