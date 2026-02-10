package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceBasicData;

import java.util.List;

/**
 * @author leon
 */
public interface DeviceServiceProvider {
    void save(Device device);

    void deleteById(Long id);

    Device findById(Long id);

    Device findByKey(String deviceKey);

    List<Device> findByKeys(List<String> deviceKey);

    Device findByIdentifier(String identifier, String integrationId);

    List<Device> findByIdentifiers(List<String> identifier, String integrationId);

    /**
     * Get all devices of an integration
     * <p><b>[Warning]</b> When the device count is high, it can be resource-intensive and time-consuming.</p>
     */
    List<Device> findAll(String integrationId);

    List<DeviceBasicData> findByIntegrations(List<String> integrationIdList);

    long countByDeviceTemplateKey(String deviceTemplateKey);

    void deleteByDeviceTemplateKey(String deviceTemplateKey);

    void clearTemplate(String deviceTemplateKey);

    boolean existsById(Long id);
}
