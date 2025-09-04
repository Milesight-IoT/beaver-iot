package com.milesight.beaveriot.device.status;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 10:27
 **/
public abstract class AbstractDeviceStatusManager {
    protected static final String IDENTIFIER_DEVICE_STATUS = "status";
    protected static final String STATUS_VALUE_ONLINE = "Online";
    protected static final String STATUS_VALUE_OFFLINE = "Offline";
    protected static final long DEFAULT_OFFLINE_SECONDS = 300;
    protected final DeviceServiceProvider deviceServiceProvider;
    protected final EntityServiceProvider entityServiceProvider;
    protected final EntityValueServiceProvider entityValueServiceProvider;
    protected final Map<String, DeviceStatusConfig> integrationDeviceStatusConfigs = new ConcurrentHashMap<>();

    public AbstractDeviceStatusManager(DeviceServiceProvider deviceServiceProvider, EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider) {
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
    }

    /**
     * Registers an integration with the device status manager.
     *
     * @param integrationId the ID of the integration
     */
    public void register(String integrationId) {
        register(integrationId, null);
    }

    /**
     * Registers an integration with the device status manager.
     *
     * @param integrationId         the ID of the integration
     * @param offlineSecondsFetcher a {@link Function} that returns the offline timeout in seconds for a given device
     *                              (e.g., returning a fixed value or calculating based on device)
     */
    public void register(String integrationId, Function<Device, Long> offlineSecondsFetcher) {
        register(integrationId, null, null, offlineSecondsFetcher);
    }

    /**
     * Registers an integration with the device status manager.
     *
     * @param integrationId         the ID of the integration
     * @param onlineUpdater         a {@link BiConsumer} that updates device status when the device is online
     *                              (e.g., setting the device's entity "status" to "online")
     * @param offlineUpdater        a {@link Consumer} that updates device status when the device is offline
     *                              (e.g., setting the device's entity "status" to "offline")
     * @param offlineSecondsFetcher a {@link Function} that returns the offline timeout in seconds for a given device
     *                              (e.g., returning a fixed value or calculating based on device)
     */
    public void register(String integrationId, BiConsumer<Device, ExchangePayload> onlineUpdater, Consumer<Device> offlineUpdater, Function<Device, Long> offlineSecondsFetcher) {
        if (onlineUpdater == null) {
            onlineUpdater = this::updateDeviceStatusToOnline;
        }
        if (offlineUpdater == null) {
            offlineUpdater = this::updateDeviceStatusToOffline;
        }
        if (offlineSecondsFetcher == null) {
            offlineSecondsFetcher = this::getDeviceOfflineSeconds;
        }
        DeviceStatusConfig config = DeviceStatusConfig.of(onlineUpdater, offlineUpdater, offlineSecondsFetcher);
        integrationDeviceStatusConfigs.put(integrationId, config);
        afterRegister(integrationId, config);
    }

    @PostConstruct
    protected abstract void init();

    @PreDestroy
    protected abstract void destroy();

    protected abstract void afterRegister(String integrationId, DeviceStatusConfig config);

    /**
     * Callback invoked when the device has successfully uploaded data.
     *
     * @param device  the device that uploaded the data
     */
    public void dataUploaded(Device device) {
        dataUploaded(device, null);
    }

    /**
     * Callback invoked when the device has successfully uploaded data.
     *
     * @param device  the device that uploaded the data
     * @param payload the exchange payload containing the uploaded data
     */
    public abstract void dataUploaded(Device device, ExchangePayload payload);

    public void updateDeviceStatusToOnline(Device device) {
        updateDeviceStatus(device, STATUS_VALUE_ONLINE);
    }

    public void updateDeviceStatusToOffline(Device device) {
        updateDeviceStatus(device, STATUS_VALUE_OFFLINE);
    }

    protected void updateDeviceStatusToOfflineByDeviceKey(String deviceKey) {
        Device device = deviceServiceProvider.findByKey(deviceKey);
        if (device == null) {
            return;
        }

        DeviceStatusConfig deviceStatusConfig = integrationDeviceStatusConfigs.get(device.getIntegrationId());
        if (deviceStatusConfig == null) {
            return;
        }

        deviceStatusConfig.getOfflineUpdater().accept(device);
    }

    protected long getDeviceOfflineSeconds(Device device) {
        return DEFAULT_OFFLINE_SECONDS;
    }

    protected void updateDeviceStatusToOnline(Device device, ExchangePayload payload) {
        updateDeviceStatus(device, STATUS_VALUE_ONLINE);
    }

    protected void updateDeviceStatus(Device device, String deviceStatus) {
        String entityKey = device.getKey() + "." + IDENTIFIER_DEVICE_STATUS;
        ExchangePayload payload = ExchangePayload.create(entityKey, deviceStatus);
        entityValueServiceProvider.saveValues(payload);
    }

    @Data
    public static class DeviceStatusConfig {
        private BiConsumer<Device, ExchangePayload> onlineUpdater;
        private Consumer<Device> offlineUpdater;
        private Function<Device, Long> offlineSecondsFetcher;

        public static DeviceStatusConfig of(BiConsumer<Device, ExchangePayload> onlineUpdater, Consumer<Device> offlineUpdater, Function<Device, Long> offlineSecondsFetcher) {
            DeviceStatusConfig config = new DeviceStatusConfig();
            config.setOnlineUpdater(onlineUpdater);
            config.setOfflineUpdater(offlineUpdater);
            config.setOfflineSecondsFetcher(offlineSecondsFetcher);
            return config;
        }
    }
}
