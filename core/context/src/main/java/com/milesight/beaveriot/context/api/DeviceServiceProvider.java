package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.Device;

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

    List<Device> findAll(String integrationId);

    long countByDeviceTemplateId(long deviceTemplateId);
}
