package com.milesight.beaveriot.device.status;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.device.status.constants.DeviceStatusConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 10:27
 **/
@Slf4j
public abstract class BaseDeviceStatusManager {
    protected final DeviceServiceProvider deviceServiceProvider;
    protected final EntityServiceProvider entityServiceProvider;
    protected final EntityValueServiceProvider entityValueServiceProvider;
    protected final EntityTemplateServiceProvider entityTemplateServiceProvider;
    protected final Map<String, DeviceStatusConfig> integrationDeviceStatusConfigs = new ConcurrentHashMap<>();

    public BaseDeviceStatusManager(DeviceServiceProvider deviceServiceProvider, EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider, EntityTemplateServiceProvider entityTemplateServiceProvider) {
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
        this.entityTemplateServiceProvider = entityTemplateServiceProvider;
    }

    public void register(String integrationId, Function<Device, Long> offlineTimeoutFetcher) {
        register(integrationId, offlineTimeoutFetcher, null);
    }

    public void register(String integrationId, Function<Device, Long> offlineTimeoutFetcher, Function<List<Device>, Map<Long, Long>> batchOfflineTimeoutFetcher) {
        DeviceStatusConfig config = DeviceStatusConfig.of(this::updateDeviceStatusToOnline, this::updateDeviceStatusToOffline, offlineTimeoutFetcher, batchOfflineTimeoutFetcher);
        integrationDeviceStatusConfigs.put(integrationId, config);
        afterRegister(integrationId, config);
    }

    @PostConstruct
    protected void init() {
        onInit();
    }

    protected abstract void onInit();

    @PreDestroy
    protected void destroy() {
        onDestroy();
    }

    protected abstract void onDestroy();

    protected abstract void afterRegister(String integrationId, DeviceStatusConfig config);

    protected void deviceOnlineCallback(Device device, Long expirationTime) {
        log.debug("Device(id={}, key={}) status updated to online, expiration time: {}", device.getId(), device.getKey(), expirationTime == null ? "-" :expirationTime / 1000);
    }

    protected void deviceOfflineCallback(Device device) {
        log.debug("Device(id={}, key={}) status updated to offline", device.getId(), device.getKey());
    }

    protected void updateDeviceStatusToOnline(Device device) {
        updateDeviceStatus(device, DeviceStatus.ONLINE.name());
    }

    protected void updateDeviceStatusToOffline(Device device) {
        updateDeviceStatus(device, DeviceStatus.OFFLINE.name());
    }

    public void offline(Device device) {
        updateDeviceStatusToOffline(device);
    }

    public DeviceStatus status(Device device) {
        String deviceStatus = (String) entityValueServiceProvider.findValueByKey(getStatusEntityKey(device));
        if (deviceStatus == null) {
            return null;
        }
        return DeviceStatus.of(deviceStatus);
    }

    protected AvailableDeviceData getAvailableDeviceDataByDeviceId(Long deviceId) {
        Device device = deviceServiceProvider.findById(deviceId);
        if (device == null) {
            return null;
        }

        DeviceStatusConfig deviceStatusConfig = integrationDeviceStatusConfigs.get(device.getIntegrationId());
        if (deviceStatusConfig == null) {
            return null;
        }

        return AvailableDeviceData.of(device, deviceStatusConfig);
    }

    protected Long getDeviceOfflineSeconds(Device device, DeviceStatusConfig config) {
        return Optional.ofNullable(config.getOfflineTimeoutFetcher())
                .map(f -> f.apply(device))
                .filter(s -> s > 0)
                .orElse(null);
    }

    protected void updateDeviceStatus(Device device, String deviceStatus) {
        String statusEntityKey = getStatusEntityKey(device);
        if (entityServiceProvider.findByKey(statusEntityKey) == null) {
            EntityTemplate entityTemplate = entityTemplateServiceProvider.findByKey(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS);
            if (entityTemplate == null) {
                throw new RuntimeException("Device status entity template not found");
            }
            Entity statusEntity = entityTemplate.toEntity(device.getIntegrationId(), device.getKey());
            entityServiceProvider.save(statusEntity);
        }

        String existValue = (String) entityValueServiceProvider.findValueByKey(statusEntityKey);
        if (!deviceStatus.equals(existValue)) {
            ExchangePayload payload = ExchangePayload.create(statusEntityKey, deviceStatus);
            entityValueServiceProvider.saveValuesAndPublishAsync(payload);
        }
    }

    protected String getStatusEntityKey(Device device) {
        return device.getKey() + "." + DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS;
    }

    @Data
    public static class DeviceStatusConfig {
        private Consumer<Device> onlineUpdater;
        private Consumer<Device> offlineUpdater;
        private Function<Device, Long> offlineTimeoutFetcher;
        private Function<List<Device>, Map<Long, Long>> batchOfflineTimeoutFetcher;

        public static DeviceStatusConfig of(Consumer<Device> onlineUpdater,
                                            Consumer<Device> offlineUpdater,
                                            Function<Device, Long> offlineTimeoutFetcher,
                                            Function<List<Device>, Map<Long, Long>> batchOfflineTimeoutFetcher) {
            DeviceStatusConfig config = new DeviceStatusConfig();
            config.setOnlineUpdater(onlineUpdater);
            config.setOfflineUpdater(offlineUpdater);
            config.setOfflineTimeoutFetcher(offlineTimeoutFetcher);
            config.setBatchOfflineTimeoutFetcher(batchOfflineTimeoutFetcher);
            return config;
        }
    }

    @Data
    public static class AvailableDeviceData {
        private Device device;
        private DeviceStatusConfig deviceStatusConfig;

        public static AvailableDeviceData of(Device device, DeviceStatusConfig deviceStatusConfig) {
            AvailableDeviceData data = new AvailableDeviceData();
            data.setDevice(device);
            data.setDeviceStatusConfig(deviceStatusConfig);
            return data;
        }
    }

    public enum DeviceStatusOperation {
        ONLINE,
        OFFLINE
    }
}
