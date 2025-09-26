package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;

import java.util.List;
import java.util.Map;
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
     * Registers an integration with the device status manager.
     *
     * @param integrationId                   the ID of the integration
     * @param offlineTimeoutFetcher           a {@link Function} that returns the offline timeout in seconds for a given device
     *                                        (e.g., returning a fixed value or calculating based on device)
     * @param batchOfflineTimeoutFetcher      a {@link Function} that returns a map of offline timeout in seconds for a given list of devices
     */
    void register(String integrationId, Function<Device, Long> offlineTimeoutFetcher, Function<List<Device>, Map<Long, Long>> batchOfflineTimeoutFetcher);
    /**
     * Updates the device status to "ONLINE",
     * then reverts to "OFFLINE" upon timeout. (If the integration was registered with the device status manager.)
     *
     * @param device the device to update
     */
    void online(Device device);
    /**
     * Updates the device status to "OFFLINE".
     *
     * @param device the device to update
     */
    void offline(Device device);
    /**
     * Returns the device status.
     *
     * @param device the device to get the status for
     * @return the device status
     */
    DeviceStatus status(Device device);
}
