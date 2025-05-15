package com.milesight.beaveriot.devicetemplate.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.model.event.DeviceTemplateEvent;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.devicetemplate.dto.DeviceTemplateDTO;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateFacade;
import com.milesight.beaveriot.devicetemplate.model.request.CreateDeviceTemplateRequest;
import com.milesight.beaveriot.devicetemplate.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.devicetemplate.model.request.UpdateDeviceTemplateRequest;
import com.milesight.beaveriot.devicetemplate.model.response.DeviceTemplateDetailResponse;
import com.milesight.beaveriot.devicetemplate.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.devicetemplate.po.DeviceTemplatePO;
import com.milesight.beaveriot.devicetemplate.repository.DeviceTemplateRepository;
import com.milesight.beaveriot.devicetemplate.support.DeviceTemplateConverter;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.permission.aspect.IntegrationPermission;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.*;

@Service
@Slf4j
public class DeviceTemplateService implements IDeviceTemplateFacade {
    @Autowired
    DeviceTemplateRepository deviceTemplateRepository;

    @Lazy
    @Autowired
    IntegrationServiceProvider integrationServiceProvider;

    @Lazy
    @Autowired
    DeviceTemplateConverter deviceTemplateConverter;

    @Lazy
    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    IUserFacade userFacade;

    @Autowired
    EventBus<DeviceTemplateEvent> eventBus;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @IntegrationPermission
    public Integration getIntegration(String integrationIdentifier) {
        return integrationServiceProvider.getIntegration(integrationIdentifier);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDeviceTemplate(CreateDeviceTemplateRequest createDeviceTemplateRequest) {
        String integrationIdentifier = createDeviceTemplateRequest.getIntegration();
        Integration integrationConfig = SpringContext.getBean(DeviceTemplateService.class).getIntegration(integrationIdentifier);

        if (integrationConfig == null) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "integration " + integrationIdentifier + " not found!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // check ability to add device template
        String addDeviceTemplateEntityId = integrationConfig.getEntityIdentifierAddDeviceTemplate();
        if (addDeviceTemplateEntityId == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "integration " + integrationIdentifier + " cannot add device template!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // call service for adding
        ExchangePayload payload = createDeviceTemplateRequest.getParamEntities();
        payload.putContext(DEVICE_TEMPLATE_NAME_ON_ADD, createDeviceTemplateRequest.getName());
        payload.putContext(DEVICE_TEMPLATE_CONTENT_ON_ADD, createDeviceTemplateRequest.getContent());
        payload.putContext(DEVICE_TEMPLATE_DESCRIPTION_ON_ADD, createDeviceTemplateRequest.getDescription());

        // Must return a device template
        try {
            entityValueServiceProvider.saveValuesAndPublishSync(payload);
        } catch (Exception e) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "add device template failed")
                    .build();
        }
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

    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceTemplate(Long deviceTemplateId, UpdateDeviceTemplateRequest updateDeviceTemplateRequest) {
        String integrationIdentifier = updateDeviceTemplateRequest.getIntegration();
        Integration integrationConfig = SpringContext.getBean(DeviceTemplateService.class).getIntegration(integrationIdentifier);

        if (integrationConfig == null) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "integration " + integrationIdentifier + " not found!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // check ability to update device template
        String updateDeviceTemplateEntityId = integrationConfig.getEntityIdentifierUpdateDeviceTemplate();
        if (updateDeviceTemplateEntityId == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "integration " + integrationIdentifier + " cannot update device template!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // call service for updating
        ExchangePayload payload = updateDeviceTemplateRequest.getParamEntities();
        payload.putContext(DEVICE_TEMPLATE_ID_ON_UPDATE, deviceTemplateId);
        payload.putContext(DEVICE_TEMPLATE_NAME_ON_UPDATE, updateDeviceTemplateRequest.getName());
        payload.putContext(DEVICE_TEMPLATE_CONTENT_ON_UPDATE, updateDeviceTemplateRequest.getContent());
        payload.putContext(DEVICE_TEMPLATE_DESCRIPTION_ON_UPDATE, updateDeviceTemplateRequest.getDescription());

        // Must return a device template
        try {
            entityValueServiceProvider.saveValuesAndPublishSync(payload);
        } catch (Exception e) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "update device template failed")
                    .build();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDeviceTemplates(List<String> deviceTemplateIdList) {
        if (deviceTemplateIdList.isEmpty()) {
            return;
        }

        List<DeviceTemplatePO> deviceTemplatePOList = deviceTemplateRepository.findByIdInWithDataPermission(deviceTemplateIdList.stream().map(Long::valueOf).toList());
        Set<String> foundIds = deviceTemplatePOList.stream().map(id -> id.getId().toString()).collect(Collectors.toSet());


        // check whether all device templates exist
        if (!new HashSet<>(deviceTemplateIdList).containsAll(foundIds)) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND)
                    .detailMessage("Some id not found!")
                    .build();
        }

        List<DeviceTemplate> deviceTemplates = deviceTemplateConverter.convertPO(deviceTemplatePOList);

        Map<String, Integration> integrationMap = getIntegrationMap(deviceTemplates.stream().map(DeviceTemplate::getIntegrationId).toList());

        deviceTemplates.stream().map((DeviceTemplate deviceTemplate) -> {
            // check ability to delete device template
            Integration integrationConfig = integrationMap.get(deviceTemplate.getIntegrationId());
            if (integrationConfig == null) {
                deleteDeviceTemplate(deviceTemplate);
                return null;
            }

            ExchangePayload payload = new ExchangePayload();
            String deleteDeviceTemplateServiceKey = integrationConfig.getEntityKeyDeleteDeviceTemplate();
            if (deleteDeviceTemplateServiceKey == null) {
                throw ServiceException
                        .with(ErrorCode.METHOD_NOT_ALLOWED)
                        .detailMessage("integration " + deviceTemplate.getIntegrationId() + " cannot delete device template!")
                        .build();
            }

            payload.put(deleteDeviceTemplateServiceKey, "");
            payload.putContext(DEVICE_TEMPLATE_ON_DELETE, deviceTemplate);
            return payload;
        }).filter(Objects::nonNull).forEach((ExchangePayload payload) -> {
            // call service for deleting
            try {
                entityValueServiceProvider.saveValuesAndPublishSync(payload);
            } catch (Exception e) {
                throw ServiceException
                        .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "delete device template failed")
                        .build();
            }
        });
    }

    public DeviceTemplateDetailResponse getDeviceTemplateDetail(Long deviceTemplateId) {
        Optional<DeviceTemplatePO> findResult = deviceTemplateRepository.findByIdWithDataPermission(deviceTemplateId);
        if (findResult.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        DeviceTemplateDetailResponse deviceTemplateDetailResponse = new DeviceTemplateDetailResponse();

        // set detail data
        BeanUtils.copyProperties(convertPOToResponseData(findResult.get()), deviceTemplateDetailResponse);
        fillIntegrationInfo(List.of(deviceTemplateDetailResponse));
        deviceTemplateDetailResponse.setIdentifier(findResult.get().getIdentifier());

        if (findResult.get().getUserId() != null) {
            List<UserDTO> userDTOList = userFacade.getUserByIds(List.of(findResult.get().getUserId()));
            if (!userDTOList.isEmpty()) {
                UserDTO user = userDTOList.get(0);
                deviceTemplateDetailResponse.setUserNickname(user.getNickname());
                deviceTemplateDetailResponse.setUserEmail(user.getEmail());
            }
        }
        return deviceTemplateDetailResponse;
    }

    // Device API Implementations

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
    public List<DeviceTemplateDTO> fuzzySearchDeviceTemplateByName(String name) {
        return convertDeviceTemplatePOList(deviceTemplateRepository
                .findAll(f -> f.likeIgnoreCase(DeviceTemplatePO.Fields.name, name))
                .stream()
                .toList());
    }

    @Override
    public List<DeviceTemplateDTO> getDeviceTemplateByIntegrations(List<String> integrationIds) {
        if (integrationIds == null || integrationIds.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDeviceTemplatePOList(deviceTemplateRepository
                .findAll(f -> f.in(DeviceTemplatePO.Fields.integration, integrationIds.toArray()))
                .stream()
                .toList());
    }

    @Override
    public List<DeviceTemplateDTO> getDeviceTemplateByIds(List<Long> deviceTemplateIds) {
        if (deviceTemplateIds == null || deviceTemplateIds.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDeviceTemplatePOList(deviceTemplateRepository.findByIdIn(deviceTemplateIds)
                .stream()
                .toList());
    }

    @Override
    public List<DeviceTemplateDTO> getDeviceTemplateByKey(List<String> deviceTemplateKeys) {
        if (deviceTemplateKeys == null || deviceTemplateKeys.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDeviceTemplatePOList(deviceTemplateRepository.findAll(f -> f.in(DeviceTemplatePO.Fields.key, deviceTemplateKeys.toArray()))
                .stream()
                .toList());
    }

    @Override
    public DeviceTemplateDTO getDeviceTemplateByKey(String deviceTemplateKey) {
        Optional<DeviceTemplatePO> deviceTemplatePO = deviceTemplateRepository.findOne(f -> f.eq(DeviceTemplatePO.Fields.key, deviceTemplateKey));
        return deviceTemplatePO.map(po -> convertDeviceTemplatePOList(List.of(po)).get(0)).orElse(null);

    }

    @Override
    public Map<String, Long> countByIntegrationIds(List<String> integrationIds) {
        List<Object[]> res = deviceTemplateRepository.countByIntegrations(integrationIds);
        Map<String, Long> result = new HashMap<>();
        res.forEach((Object[] o) -> result.put((String) o[0], (Long) o[1]));
        return result;
    }

    @Override
    public Long countByIntegrationId(String integrationId) {
        return deviceTemplateRepository.count(f -> f.eq(DeviceTemplatePO.Fields.integration, integrationId));
    }

    public void deleteDeviceTemplate(DeviceTemplate deviceTemplate) {
        entityServiceProvider.deleteByTargetId(deviceTemplate.getId().toString());

        deviceTemplateRepository.deleteById(deviceTemplate.getId());

        eventBus.publish(DeviceTemplateEvent.of(DeviceTemplateEvent.EventType.DELETED, deviceTemplate));
    }
}
