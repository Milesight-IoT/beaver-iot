package com.milesight.beaveriot.devicetemplate.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.devicetemplate.dto.DeviceTemplateDTO;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateFacade;
import com.milesight.beaveriot.devicetemplate.po.DeviceTemplatePO;
import com.milesight.beaveriot.devicetemplate.repository.DeviceTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceTemplateService implements IDeviceTemplateFacade {
    private final DeviceTemplateRepository deviceTemplateRepository;

    private final IntegrationServiceProvider integrationServiceProvider;

    private final DeviceServiceProvider deviceServiceProvider;

    private final EntityServiceProvider entityServiceProvider;

    public DeviceTemplateService(DeviceTemplateRepository deviceTemplateRepository, @Lazy IntegrationServiceProvider integrationServiceProvider, @Lazy DeviceServiceProvider deviceServiceProvider, @Lazy EntityServiceProvider entityServiceProvider) {
        this.deviceTemplateRepository = deviceTemplateRepository;
        this.integrationServiceProvider = integrationServiceProvider;
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
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
            responseDataList = deviceTemplateRepository
                    .findAllWithDataPermission(f -> f.likeIgnoreCase(StringUtils.hasText(searchDeviceTemplateRequest.getName()), DeviceTemplatePO.Fields.name, searchDeviceTemplateRequest.getName()), searchDeviceTemplateRequest.toPageable())
                    .map(this::convertPOToResponseData);
        }catch (Exception e) {
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

    public void deleteDeviceTemplate(DeviceTemplate deviceTemplate) {
        entityServiceProvider.deleteByTargetId(deviceTemplate.getId().toString());

        deviceTemplateRepository.deleteById(deviceTemplate.getId());

        deviceServiceProvider.clearTemplate(deviceTemplate.getKey());
    }
}
