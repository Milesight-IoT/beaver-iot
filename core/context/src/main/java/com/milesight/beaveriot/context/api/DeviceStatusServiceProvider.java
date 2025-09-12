package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.Device;

import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 11:04
 **/
public interface DeviceStatusServiceProvider {
    /**
     * Registers an integration with the device status manager.
     *
     * @param integrationId         the ID of the integration
     * @param offlineTimeoutFetcher a {@link Function} that returns the offline timeout in seconds for a given device
     *                              (e.g., returning a fixed value or calculating based on device)
     */
    void register(String integrationId, Function<Device, Long> offlineTimeoutFetcher);
    /**
     * Updates the device status to "Online".
     *
     * @param device the device to update
     */
    void online(Device device);
    /**
     * Updates the device status to "Offline".
     *
     * @param device the device to update
     */
    void offline(Device device);
}
