package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 11:04
 **/
public interface DeviceStatusServiceProvider {
    /**
     * Registers an integration with the device status manager.
     *
     * @param integrationId the ID of the integration
     */
    void register(String integrationId);
    /**
     * Registers an integration with the device status manager.
     *
     * @param integrationId         the ID of the integration
     * @param offlineSecondsFetcher a {@link Function} that returns the offline timeout in seconds for a given device
     *                              (e.g., returning a fixed value or calculating based on device)
     */
    void register(String integrationId, Function<Device, Long> offlineSecondsFetcher);
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
    void register(String integrationId, BiConsumer<Device, ExchangePayload> onlineUpdater, Consumer<Device> offlineUpdater, Function<Device, Long> offlineSecondsFetcher);
    /**
     * Callback invoked when the device has successfully uploaded data.
     *
     * @param device  the device that uploaded the data
     */
    void dataUploaded(Device device);
    /**
     * Callback invoked when the device has successfully uploaded data.
     *
     * @param device  the device that uploaded the data
     * @param payload the exchange payload containing the uploaded data
     */
    void dataUploaded(Device device, ExchangePayload payload);
    /**
     * Updates the device status to "Online".
     *
     * @param device the device to update
     */
    void updateDeviceStatusToOnline(Device device);
    /**
     * Updates the device status to "Offline".
     *
     * @param device the device to update
     */
    void updateDeviceStatusToOffline(Device device);
    /**
     * Release the resources when a device is deleted.
     *
     * @param device the device that was deleted
     */
    void deviceDeleted(Device device);
}
