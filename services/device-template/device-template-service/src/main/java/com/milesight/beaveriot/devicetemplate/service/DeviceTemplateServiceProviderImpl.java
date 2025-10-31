package com.milesight.beaveriot.devicetemplate.service;

import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceTemplateServiceProviderImpl implements DeviceTemplateServiceProvider {
    private final DeviceTemplateService deviceTemplateService;

    public DeviceTemplateServiceProviderImpl(DeviceTemplateService deviceTemplateService) {
        this.deviceTemplateService = deviceTemplateService;
    }

    @Override
    public void save(DeviceTemplate deviceTemplate) {
        deviceTemplateService.save(deviceTemplate);
    }

    @Override
    public void deleteById(Long id) {
        deviceTemplateService.deleteById(id);
    }

    @Override
    public void deleteByKey(String key) {
        deviceTemplateService.deleteByKey(key);
    }

    @Override
    public DeviceTemplate findById(Long id) {
        return deviceTemplateService.findById(id);
    }

    @Override
    public List<DeviceTemplate> findByIds(List<Long> ids) {
        return deviceTemplateService.findByIds(ids);
    }

    @Override
    public DeviceTemplate findByKey(String deviceTemplateKey) {
        return deviceTemplateService.findByKey(deviceTemplateKey);
    }

    @Override
    public List<DeviceTemplate> findByKeys(List<String> deviceTemplateKey) {
        return deviceTemplateService.findByKeys(deviceTemplateKey);
    }

    @Override
    public DeviceTemplate findByIdentifier(String identifier, String integrationId) {
        return deviceTemplateService.findByIdentifier(identifier, integrationId);
    }

    @Override
    public List<DeviceTemplate> findByIdentifiers(List<String> identifier, String integrationId) {
        return deviceTemplateService.findByIdentifiers(identifier, integrationId);
    }

    @Override
    public List<DeviceTemplate> findAll(String integrationId) {
        return deviceTemplateService.findAll(integrationId);
    }

    @Override
    public List<DeviceTemplate> findAllCustom(String integrationId) {
        return deviceTemplateService.findAllCustom(integrationId);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        deviceTemplateService.batchDelete(ids);
    }

    @Override
    public Page<DeviceTemplateResponseData> search(SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        return deviceTemplateService.search(searchDeviceTemplateRequest);
    }
}
