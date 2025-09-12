package com.milesight.beaveriot.device.status;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
    protected static final String IDENTIFIER_DEVICE_STATUS = "status";
    protected static final String STATUS_VALUE_ONLINE = "Online";
    protected static final String STATUS_VALUE_OFFLINE = "Offline";
    protected static final long DEFAULT_OFFLINE_SECONDS = 300;
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
        if (offlineTimeoutFetcher == null) {
            offlineTimeoutFetcher = this::getDeviceDefaultOfflineSeconds;
        }
        DeviceStatusConfig config = DeviceStatusConfig.of(this::updateDeviceStatusToOnline, this::updateDeviceStatusToOffline, offlineTimeoutFetcher);
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

    protected void deviceOnlineCallback(Device device, long expirationTime) {
        log.debug("Device(id={}, key={}) status updated to online, expiration time: {}", device.getId(), device.getKey(), expirationTime / 1000);
    }

    protected void deviceOfflineCallback(Device device) {
        log.debug("Device(id={}, key={}) status updated to offline", device.getId(), device.getKey());
    }

    protected void updateDeviceStatusToOnline(Device device) {
        updateDeviceStatus(device, STATUS_VALUE_ONLINE);
    }

    protected void updateDeviceStatusToOffline(Device device) {
        updateDeviceStatus(device, STATUS_VALUE_OFFLINE);
    }

    public void offline(Device device) {
        updateDeviceStatusToOffline(device);
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

    protected long getDeviceOfflineSeconds(Device device, DeviceStatusConfig config) {
        return Optional.ofNullable(config.getOfflineTimeoutFetcher())
                .map(f -> f.apply(device))
                .orElse(DEFAULT_OFFLINE_SECONDS);
    }

    protected long getDeviceDefaultOfflineSeconds(Device device) {
        return DEFAULT_OFFLINE_SECONDS;
    }

    protected void updateDeviceStatus(Device device, String deviceStatus) {
        String entityKey = device.getKey() + "." + IDENTIFIER_DEVICE_STATUS;
        if (entityServiceProvider.findByKey(entityKey) == null) {
            EntityTemplate entityTemplate = entityTemplateServiceProvider.findByKey(IDENTIFIER_DEVICE_STATUS);
            if (entityTemplate == null) {
                throw new RuntimeException("Device status entity template not found");
            }
            Entity statusEntity = entityTemplate.toEntity(device.getIntegrationId(), device.getKey());
            entityServiceProvider.save(statusEntity);
        }
        ExchangePayload payload = ExchangePayload.create(entityKey, deviceStatus);
        entityValueServiceProvider.saveValues(payload);
    }

    @Data
    public static class DeviceStatusConfig {
        private Consumer<Device> onlineUpdater;
        private Consumer<Device> offlineUpdater;
        private Function<Device, Long> offlineTimeoutFetcher;

        public static DeviceStatusConfig of(Consumer<Device> onlineUpdater, Consumer<Device> offlineUpdater, Function<Device, Long> offlineTimeoutFetcher) {
            DeviceStatusConfig config = new DeviceStatusConfig();
            config.setOnlineUpdater(onlineUpdater);
            config.setOfflineUpdater(offlineUpdater);
            config.setOfflineTimeoutFetcher(offlineTimeoutFetcher);
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
}
