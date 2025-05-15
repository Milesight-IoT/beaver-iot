package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.DeviceTemplate;

import java.util.List;

/**
 * @author leon
 */
public interface DeviceTemplateServiceProvider {
    void save(DeviceTemplate deviceTemplate);

    void deleteById(Long id);

    DeviceTemplate findById(Long id);

    DeviceTemplate findByKey(String deviceTemplateKey);

    List<DeviceTemplate> findByKeys(List<String> deviceTemplateKey);

    DeviceTemplate findByIdentifier(String identifier, String integrationId);

    List<DeviceTemplate> findByIdentifiers(List<String> identifier, String integrationId);

    List<DeviceTemplate> findAll(String integrationId);
}
