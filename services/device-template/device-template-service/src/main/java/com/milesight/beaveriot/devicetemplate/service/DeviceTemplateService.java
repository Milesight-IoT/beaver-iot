package com.milesight.beaveriot.devicetemplate.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.devicetemplate.dto.DeviceTemplateDTO;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateFacade;
import com.milesight.beaveriot.devicetemplate.po.DeviceTemplatePO;
import com.milesight.beaveriot.devicetemplate.repository.DeviceTemplateRepository;
import com.milesight.beaveriot.devicetemplate.support.DeviceTemplateConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceTemplateService implements IDeviceTemplateFacade {
    private final DeviceTemplateRepository deviceTemplateRepository;
    private final DeviceTemplateConverter deviceTemplateConverter;
    private final IntegrationServiceProvider integrationServiceProvider;
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityServiceProvider entityServiceProvider;

    public DeviceTemplateService(DeviceTemplateRepository deviceTemplateRepository, DeviceTemplateConverter deviceTemplateConverter, @Lazy IntegrationServiceProvider integrationServiceProvider, @Lazy DeviceServiceProvider deviceServiceProvider, @Lazy EntityServiceProvider entityServiceProvider) {
        this.deviceTemplateRepository = deviceTemplateRepository;
        this.deviceTemplateConverter = deviceTemplateConverter;
        this.integrationServiceProvider = integrationServiceProvider;
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
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

        // set device template data
        if (!deviceTemplate.getName().equals(deviceTemplatePO.getName())) {
            deviceTemplatePO.setName(deviceTemplate.getName());
            shouldUpdate = true;
        }
        if (!deviceTemplate.getContent().equals(deviceTemplatePO.getContent())) {
            deviceTemplatePO.setContent(deviceTemplate.getContent());
            shouldUpdate = true;
        }
        if (!Objects.equals(deviceTemplate.getDescription(), deviceTemplatePO.getDescription())) {
            deviceTemplatePO.setDescription(deviceTemplate.getDescription());
            shouldUpdate = true;
        }

        if (!deviceTemplateAdditionalDataEqual(deviceTemplate.getAdditional(), deviceTemplatePO.getAdditionalData())) {
            deviceTemplatePO.setAdditionalData(deviceTemplate.getAdditional());
            shouldUpdate = true;
        }

        if (!Objects.equals(deviceTemplate.getVendor(), deviceTemplatePO.getVendor())) {
            deviceTemplatePO.setVendor(deviceTemplate.getVendor());
            shouldUpdate = true;
        }

        if (!Objects.equals(deviceTemplate.getModel(), deviceTemplatePO.getModel())) {
            deviceTemplatePO.setModel(deviceTemplate.getModel());
            shouldUpdate = true;
        }

        if (!Objects.equals(deviceTemplate.getBlueprintLibraryId(), deviceTemplatePO.getBlueprintLibraryId())) {
            deviceTemplatePO.setBlueprintLibraryId(deviceTemplate.getBlueprintLibraryId());
            shouldUpdate = true;
        }

        if (!Objects.equals(deviceTemplate.getBlueprintLibraryVersion(), deviceTemplatePO.getBlueprintLibraryVersion())) {
            deviceTemplatePO.setBlueprintLibraryVersion(deviceTemplate.getBlueprintLibraryVersion());
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

    private DeviceTemplateService self() {
        return (DeviceTemplateService) AopContext.currentProxy();
    }

    @Override
    public void deleteById(Long id) {
        DeviceTemplate deviceTemplate = findById(id);
        Assert.notNull(deviceTemplate, "Delete failed. Cannot find device template " + id.toString());
        self().deleteDeviceTemplate(deviceTemplate);
    }

    @Override
    public void deleteByKey(String key) {
        DeviceTemplate deviceTemplate = findByKey(key);
        Assert.notNull(deviceTemplate, "Delete failed. Cannot find device template " + key);
        self().deleteDeviceTemplate(deviceTemplate);
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

    public DeviceTemplate findByBlueprintLibrary(Long blueprintLibraryId, String blueprintLibraryVersion, String vendor, String model) {
        List<DeviceTemplatePO> deviceTemplatePOs = deviceTemplateRepository.findByBlueprintLibraryIdAndBlueprintLibraryVersionAndVendorAndModel(blueprintLibraryId, blueprintLibraryVersion, vendor, model);
        if (CollectionUtils.isEmpty(deviceTemplatePOs)) {
            return null;
        }

        return deviceTemplateConverter.convertPO(deviceTemplatePOs.get(0));
    }

    public List<DeviceTemplate> findByBlueprintLibraryIgnoreTenant(Long blueprintLibraryId, String blueprintLibraryVersion) {
        return deviceTemplateConverter.convertPO(deviceTemplateRepository.findByBlueprintLibraryIdAndBlueprintLibraryVersionIgnoreTenant(blueprintLibraryId, blueprintLibraryVersion));
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
    public List<DeviceTemplate> findAllCustom(String integrationId) {
        return deviceTemplateConverter.convertPO(deviceTemplateRepository
                .findAll(f -> f.eq(DeviceTemplatePO.Fields.integration, integrationId)
                        .and(f2 -> f2.isNull(DeviceTemplatePO.Fields.blueprintLibraryId))));
    }

    @Override
    public Page<DeviceTemplateResponseData> search(SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        return searchDeviceTemplate(searchDeviceTemplateRequest);
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

    private DeviceTemplateResponseData convertPOToResponseData(DeviceTemplatePO deviceTemplatePO) {
        DeviceTemplateResponseData deviceTemplateResponseData = new DeviceTemplateResponseData();
        deviceTemplateResponseData.setId(deviceTemplatePO.getId().toString());
        deviceTemplateResponseData.setKey(deviceTemplatePO.getKey());
        deviceTemplateResponseData.setName(deviceTemplatePO.getName());
        deviceTemplateResponseData.setContent(deviceTemplatePO.getContent());
        deviceTemplateResponseData.setDescription(deviceTemplatePO.getDescription());
        deviceTemplateResponseData.setIntegration(deviceTemplatePO.getIntegration());
        deviceTemplateResponseData.setAdditionalData(deviceTemplatePO.getAdditionalData());
        deviceTemplateResponseData.setCreatedAt(deviceTemplatePO.getCreatedAt());
        deviceTemplateResponseData.setUpdatedAt(deviceTemplatePO.getUpdatedAt());

        deviceTemplateResponseData.setDeviceCount(deviceServiceProvider.countByDeviceTemplateKey(deviceTemplatePO.getKey()));

        return deviceTemplateResponseData;
    }

    private Map<String, Integration> getIntegrationMap(List<String> identifiers) {
        Set<String> integrationIdentifiers = new HashSet<>(identifiers);
        return integrationServiceProvider
                .findIntegrations(f -> integrationIdentifiers.contains(f.getId()))
                .stream()
                .collect(Collectors.toMap(Integration::getId, integration -> integration));
    }

    private void fillIntegrationInfo(List<DeviceTemplateResponseData> dataList) {
        Map<String, Integration> integrationMap = getIntegrationMap(dataList.stream().map(DeviceTemplateResponseData::getIntegration).toList());
        dataList.forEach(d -> {
            Integration integration = integrationMap.get(d.getIntegration());
            if (integration == null) {
                d.setDeletable(true);
                return;
            }

            d.setDeletable(true);
            d.setIntegrationName(integration.getName());
        });
    }

    public Page<DeviceTemplateResponseData> searchDeviceTemplate(SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        if (searchDeviceTemplateRequest.getSort().getOrders().isEmpty()) {
            searchDeviceTemplateRequest.sort(new Sorts().desc(DeviceTemplatePO.Fields.id));
        }

        Page<DeviceTemplateResponseData> responseDataList;
        try {
            Consumer<Filterable> filterable = f -> {
                f.likeIgnoreCase(
                                StringUtils.hasText(searchDeviceTemplateRequest.getName()),
                                DeviceTemplatePO.Fields.name,
                                searchDeviceTemplateRequest.getName()
                        );

                if (searchDeviceTemplateRequest.getDeviceTemplateSource() == SearchDeviceTemplateRequest.DeviceTemplateSource.CUSTOM) {
                    f.and(f2 -> f2.isNull(DeviceTemplatePO.Fields.blueprintLibraryId));
                } else if (searchDeviceTemplateRequest.getDeviceTemplateSource() == SearchDeviceTemplateRequest.DeviceTemplateSource.BLUEPRINT_LIBRARY) {
                    f.and(f2 -> f2.isNotNull(DeviceTemplatePO.Fields.blueprintLibraryId));
                }

                if (!CollectionUtils.isEmpty(searchDeviceTemplateRequest.getDeviceTemplateIds())) {
                    f.and(f3 -> f3.in(DeviceTemplatePO.Fields.id, searchDeviceTemplateRequest.getDeviceTemplateIds().toArray()));
                }
            };

            responseDataList = deviceTemplateRepository
                    .findAllWithDataPermission(
                            filterable,
                            searchDeviceTemplateRequest.toPageable()
                    )
                    .map(this::convertPOToResponseData);
        } catch (Exception e) {
            if (e instanceof ServiceException && Objects.equals(((ServiceException) e).getErrorCode(), ErrorCode.FORBIDDEN_PERMISSION.getErrorCode())) {
                return Page.empty();
            }
            throw e;
        }

        fillIntegrationInfo(responseDataList.stream().toList());
        return responseDataList;
    }

    private List<DeviceTemplateDTO> convertDeviceTemplatePOList(List<DeviceTemplatePO> DeviceTemplatePOList) {
        Map<String, Integration> integrationMap = getIntegrationMap(DeviceTemplatePOList.stream().map(DeviceTemplatePO::getIntegration).toList());
        return DeviceTemplatePOList.stream().map(deviceTemplatePO -> DeviceTemplateDTO.builder()
                .id(deviceTemplatePO.getId())
                .name(deviceTemplatePO.getName())
                .content(deviceTemplatePO.getContent())
                .description(deviceTemplatePO.getDescription())
                .key(deviceTemplatePO.getKey())
                .userId(deviceTemplatePO.getUserId())
                .createdAt(deviceTemplatePO.getCreatedAt())
                .integrationId(deviceTemplatePO.getIntegration())
                .integrationConfig(integrationMap.get(deviceTemplatePO.getIntegration()))
                .build()
        ).toList();
    }

    @Override
    public List<DeviceTemplateDTO> getDeviceTemplateByKeys(List<String> deviceTemplateKeys) {
        if (deviceTemplateKeys == null || deviceTemplateKeys.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDeviceTemplatePOList(deviceTemplateRepository.findAll(f -> f.in(DeviceTemplatePO.Fields.key, deviceTemplateKeys.toArray()))
                .stream()
                .toList());
    }

    @Transactional
    public void deleteDeviceTemplate(DeviceTemplate deviceTemplate) {
        entityServiceProvider.deleteByTargetId(deviceTemplate.getId().toString());

        deviceTemplateRepository.deleteById(deviceTemplate.getId());

        deviceServiceProvider.deleteByDeviceTemplateKey(deviceTemplate.getKey());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDeviceTemplateByIdInIgnoreTenant(List<Long> ids) {
        deviceTemplateRepository.deleteByIdInIgnoreTenant(ids);
    }
}
