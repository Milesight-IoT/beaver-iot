package com.milesight.beaveriot.devicetemplate.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.devicetemplate.po.DeviceTemplatePO;
import com.milesight.beaveriot.devicetemplate.repository.DeviceTemplateRepository;
import com.milesight.beaveriot.devicetemplate.support.DeviceTemplateConverter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeviceTemplateServiceProviderImpl implements DeviceTemplateServiceProvider {
    private final DeviceTemplateRepository deviceTemplateRepository;

    private final DeviceTemplateConverter deviceTemplateConverter;

    private final EntityServiceProvider entityServiceProvider;

    private final DeviceTemplateService deviceTemplateService;

    public DeviceTemplateServiceProviderImpl(DeviceTemplateRepository deviceTemplateRepository, DeviceTemplateConverter deviceTemplateConverter, EntityServiceProvider entityServiceProvider, DeviceTemplateService deviceTemplateService) {
        this.deviceTemplateRepository = deviceTemplateRepository;
        this.deviceTemplateConverter = deviceTemplateConverter;
        this.entityServiceProvider = entityServiceProvider;
        this.deviceTemplateService = deviceTemplateService;
    }

    @Override
    public void save(DeviceTemplate deviceTemplate) {
        Long userId = SecurityUserContext.getUserId();

        DeviceTemplatePO deviceTemplatePO;
        Assert.notNull(deviceTemplate.getName(), "Device Template Name must be provided!");
        Assert.notNull(deviceTemplate.getContent(), "Device Template Content must be provided!");
        Assert.notNull(deviceTemplate.getIdentifier(), "Device Template identifier must be provided!");
        Assert.notNull(deviceTemplate.getIntegrationId(), "Integration must be provided!");

        boolean shouldCreate = false;
        boolean shouldUpdate = false;

        // check id
        if (deviceTemplate.getId() != null) {
            deviceTemplatePO = deviceTemplateRepository.findById(deviceTemplate.getId()).orElse(null);
            if (deviceTemplatePO == null) {
                deviceTemplatePO = new DeviceTemplatePO();
                deviceTemplatePO.setId(deviceTemplate.getId());
                shouldCreate = true;
            }
        } else {
            deviceTemplatePO = deviceTemplateRepository
                    .findOne(f -> f
                            .eq(DeviceTemplatePO.Fields.identifier, deviceTemplate.getIdentifier())
                            .eq(DeviceTemplatePO.Fields.integration, deviceTemplate.getIntegrationId())
                    ).orElse(null);
            if (deviceTemplatePO == null) {
                deviceTemplatePO = new DeviceTemplatePO();
                deviceTemplatePO.setId(SnowflakeUtil.nextId());
                shouldCreate = true;
            }
        }

        // set device data
        if (!deviceTemplate.getName().equals(deviceTemplatePO.getName())) {
            deviceTemplatePO.setName(deviceTemplate.getName());
            shouldUpdate = true;
        }
        if (!deviceTemplate.getContent().equals(deviceTemplatePO.getContent())) {
            deviceTemplatePO.setContent(deviceTemplate.getContent());
            shouldUpdate = true;
        }
        if (!deviceTemplate.getDescription().equals(deviceTemplatePO.getDescription())) {
            deviceTemplatePO.setDescription(deviceTemplate.getDescription());
            shouldUpdate = true;
        }

        if (!deviceTemplateAdditionalDataEqual(deviceTemplate.getAdditional(), deviceTemplatePO.getAdditionalData())) {
            deviceTemplatePO.setAdditionalData(deviceTemplate.getAdditional());
            shouldUpdate = true;
        }

        // create or update
        if (shouldCreate) {
            deviceTemplatePO.setUserId(userId);
            // integration / identifier / key would not be updated
            deviceTemplatePO.setIntegration(deviceTemplate.getIntegrationId());
            deviceTemplatePO.setIdentifier(deviceTemplate.getIdentifier());
            deviceTemplatePO.setKey(deviceTemplate.getKey());
            deviceTemplatePO = deviceTemplateRepository.save(deviceTemplatePO);
        } else if (shouldUpdate) {
            deviceTemplatePO = deviceTemplateRepository.save(deviceTemplatePO);
        }

        deviceTemplate.setId(deviceTemplatePO.getId());
    }

    @Override
    public void deleteById(Long id) {
        DeviceTemplate deviceTemplate = findById(id);
        Assert.notNull(deviceTemplate, "Delete failed. Cannot find device template " + id.toString());
        deviceTemplateService.deleteDeviceTemplate(deviceTemplate);
    }

    @Override
    public void deleteByKey(String key) {
        DeviceTemplate deviceTemplate = findByKey(key);
        Assert.notNull(deviceTemplate, "Delete failed. Cannot find device template " + key);
        deviceTemplateService.deleteDeviceTemplate(deviceTemplate);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchDelete(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }

        List<DeviceTemplatePO> deviceTemplatePOList = deviceTemplateRepository.findByIdInWithDataPermission(ids.stream().toList());
        Set<Long> foundIds = deviceTemplatePOList.stream().map(DeviceTemplatePO::getId).collect(Collectors.toSet());

        // check whether all device templates exist
        if (!new HashSet<>(ids).containsAll(foundIds)) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND)
                    .detailMessage("Some id not found!")
                    .build();
        }

        List<DeviceTemplate> deviceTemplates = deviceTemplateConverter.convertPO(deviceTemplatePOList);

        deviceTemplates.forEach(this::deleteDeviceTemplate);
    }

    public void deleteDeviceTemplate(DeviceTemplate deviceTemplate) {
        entityServiceProvider.deleteByTargetId(deviceTemplate.getId().toString());

        deviceTemplateRepository.deleteById(deviceTemplate.getId());
    }

    @Override
    public DeviceTemplate findById(Long id) {
        return deviceTemplateRepository
                .findOne(f -> f
                        .eq(DeviceTemplatePO.Fields.id, id)
                )
                .map(deviceTemplateConverter::convertPO)
                .orElse(null);
    }

    @Override
    public List<DeviceTemplate> findByIds(List<Long> ids) {
        if (ObjectUtils.isEmpty(ids)) {
            return List.of();
        }

        return deviceTemplateConverter.convertPO(deviceTemplateRepository
                .findAll(f -> f
                        .in(DeviceTemplatePO.Fields.id, ids.toArray())
                ));
    }

    @Override
    public DeviceTemplate findByKey(String deviceTemplateKey) {
        return deviceTemplateRepository
                .findOne(f -> f
                        .eq(DeviceTemplatePO.Fields.key, deviceTemplateKey)
                )
                .map(deviceTemplateConverter::convertPO)
                .orElse(null);
    }

    @Override
    public List<DeviceTemplate> findByKeys(List<String> deviceTemplateKeys) {
        if (ObjectUtils.isEmpty(deviceTemplateKeys)) {
            return List.of();
        }

        return deviceTemplateConverter.convertPO(deviceTemplateRepository
                .findAll(f -> f
                        .in(DeviceTemplatePO.Fields.key, deviceTemplateKeys.toArray())
                ));
    }

    @Override
    public DeviceTemplate findByIdentifier(String identifier, String integrationId) {
        return deviceTemplateRepository
                .findOne(f -> f
                        .eq(DeviceTemplatePO.Fields.identifier, identifier)
                        .eq(DeviceTemplatePO.Fields.integration, integrationId)
                )
                .map(deviceTemplateConverter::convertPO)
                .orElse(null);
    }

    @Override
    public List<DeviceTemplate> findByIdentifiers(List<String> identifiers, String integrationId) {
        if (ObjectUtils.isEmpty(identifiers)) {
            return List.of();
        }

        return deviceTemplateConverter.convertPO(deviceTemplateRepository
                .findAll(f -> f
                        .in(DeviceTemplatePO.Fields.identifier, identifiers.toArray())
                        .eq(DeviceTemplatePO.Fields.integration, integrationId)
                ));
    }

    @Override
    public List<DeviceTemplate> findAll(String integrationId) {
        return deviceTemplateConverter.convertPO(deviceTemplateRepository
                .findAll(f -> f.eq(DeviceTemplatePO.Fields.integration, integrationId)));
    }

    @Override
    public Page<DeviceTemplateResponseData> search(SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        return deviceTemplateService.searchDeviceTemplate(searchDeviceTemplateRequest);
    }

    private boolean deviceTemplateAdditionalDataEqual(Map<String, Object> arg1, Map<String, Object> arg2) {
        if (arg1 == null && arg2 == null) {
            return true;
        }

        if (arg1 == null || arg2 == null) {
            return false;
        }

        return arg1.equals(arg2);
    }
}
