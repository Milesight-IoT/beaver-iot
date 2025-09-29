package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.context.integration.model.DeviceStatusConfig;

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
     * @param config        configuration object containing the following:
     *                      <ul>
     *                        <li><b>offlineTimeoutFetcher:</b> a {@link Function} that returns the offline timeout in seconds for a given device
     *                          (e.g., returning a fixed value or calculating based on device)</li>
     *                        <li><b>batchOfflineTimeoutFetcher:</b> a {@link Function} that returns a map of offline timeout in seconds for a given list of devices</li>
     *                        <li><b>onlineListener:</b> a {@link Consumer} that is called when a device goes online</li>
     *                        <li><b>offlineListener:</b> a {@link Consumer} that is called when a device goes offline</li>
     *                      </ul>
     * @see DeviceStatusConfig
     */
    void register(String integrationId, DeviceStatusConfig config);
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
