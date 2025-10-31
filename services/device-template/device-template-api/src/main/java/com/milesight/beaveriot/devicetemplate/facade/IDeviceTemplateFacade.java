package com.milesight.beaveriot.devicetemplate.facade;

import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.devicetemplate.dto.DeviceTemplateDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IDeviceTemplateFacade {
    void save(DeviceTemplate deviceTemplate);
    void deleteById(Long id);
    void deleteByKey(String key);
    DeviceTemplate findById(Long id);
    List<DeviceTemplate> findByIds(List<Long> ids);
    DeviceTemplate findByKey(String deviceTemplateKey);
    List<DeviceTemplate> findByKeys(List<String> deviceTemplateKey);
    DeviceTemplate findByIdentifier(String identifier, String integrationId);
    List<DeviceTemplate> findByIdentifiers(List<String> identifier, String integrationId);
    DeviceTemplate findByBlueprintLibrary(Long blueprintLibraryId, String blueprintLibraryVersion, String vendor, String model);
    List<DeviceTemplate> findByBlueprintLibraryIgnoreTenant(Long blueprintLibraryId, String blueprintLibraryVersion);
    List<DeviceTemplate> findAll(String integrationId);
    List<DeviceTemplate> findAllCustom(String integrationId);
    void batchDelete(List<Long> ids);
    Page<DeviceTemplateResponseData> search(SearchDeviceTemplateRequest searchDeviceTemplateRequest);
    List<DeviceTemplateDTO> getDeviceTemplateByKeys(List<String> deviceTemplateKeys);
    void deleteDeviceTemplateByIdInIgnoreTenant(List<Long> ids);
}
