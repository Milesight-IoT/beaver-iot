package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.device.model.request.CreateDeviceRequest;
import com.milesight.beaveriot.device.model.request.SearchDeviceRequest;
import com.milesight.beaveriot.device.model.request.UpdateDeviceRequest;
import com.milesight.beaveriot.device.model.response.DeviceDetailResponse;
import com.milesight.beaveriot.device.model.response.DeviceEntityData;
import com.milesight.beaveriot.device.model.response.DeviceResponseData;
import com.milesight.beaveriot.device.po.DevicePO;
import com.milesight.beaveriot.device.repository.DeviceRepository;
import com.milesight.beaveriot.device.support.DeviceConverter;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.permission.aspect.IntegrationPermission;
import com.milesight.beaveriot.user.enums.ResourceType;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.DEVICE_NAME_ON_ADD;
import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.DEVICE_ON_DELETE;

@Service
@Slf4j
public class DeviceService implements IDeviceFacade {
    @Autowired
    DeviceRepository deviceRepository;

    @Lazy
    @Autowired
    IntegrationServiceProvider integrationServiceProvider;

    @Lazy
    @Autowired
    DeviceConverter deviceConverter;

    @Lazy
    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    IUserFacade userFacade;

    @Autowired
    EventBus<DeviceEvent> eventBus;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @IntegrationPermission
    public Integration getIntegration(String integrationIdentifier) {
        return integrationServiceProvider.getIntegration(integrationIdentifier);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDevice(CreateDeviceRequest createDeviceRequest) {
        String integrationIdentifier = createDeviceRequest.getIntegration();
        Integration integrationConfig = SpringContext.getBean(DeviceService.class).getIntegration(integrationIdentifier);

        if (integrationConfig == null) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "integration " + integrationIdentifier + " not found!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // check ability to add device
        String addDeviceEntityId = integrationConfig.getEntityIdentifierAddDevice();
        if (addDeviceEntityId == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "integration " + integrationIdentifier + " cannot add device!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // call service for adding
        ExchangePayload payload = createDeviceRequest.getParamEntities();
        payload.putContext(DEVICE_NAME_ON_ADD, createDeviceRequest.getName());

        // Must return a device
        try {
            entityValueServiceProvider.saveValuesAndPublishSync(payload);
        } catch (Exception e) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "add device failed")
                    .build();
        }
    }

    private DeviceResponseData convertPOToResponseData(DevicePO devicePO) {
        DeviceResponseData deviceResponseData = new DeviceResponseData();
        deviceResponseData.setId(devicePO.getId().toString());
        deviceResponseData.setKey(devicePO.getKey());
        deviceResponseData.setName(devicePO.getName());
        deviceResponseData.setIntegration(devicePO.getIntegration());
        deviceResponseData.setAdditionalData(devicePO.getAdditionalData());
        deviceResponseData.setCreatedAt(devicePO.getCreatedAt());
        deviceResponseData.setUpdatedAt(devicePO.getUpdatedAt());

        return deviceResponseData;
    }

    private Map<String, Integration> getIntegrationMap(List<String> identifiers) {
        Set<String> integrationIdentifiers = new HashSet<>(identifiers);
        return integrationServiceProvider
                .findIntegrations(f -> integrationIdentifiers.contains(f.getId()))
                .stream()
                .collect(Collectors.toMap(Integration::getId, integration -> integration));
    }

    private void fillIntegrationInfo(List<DeviceResponseData> dataList) {
        Map<String, Integration> integrationMap = getIntegrationMap(dataList.stream().map(DeviceResponseData::getIntegration).toList());
        dataList.forEach(d -> {
            Integration integration = integrationMap.get(d.getIntegration());
            if (integration == null) {
                d.setDeletable(true);
                return;
            }

            d.setDeletable(StringUtils.hasLength(integration.getEntityIdentifierDeleteDevice()));
            d.setIntegrationName(integration.getName());
        });
    }

    public Page<DeviceResponseData> searchDevice(SearchDeviceRequest searchDeviceRequest) {
        // convert to `DeviceResponseData`
        if (searchDeviceRequest.getSort().getOrders().isEmpty()) {
            searchDeviceRequest.sort(new Sorts().desc(DevicePO.Fields.id));
        }

        Page<DeviceResponseData> responseDataList;
        try {
            responseDataList = deviceRepository
                    .findAllWithDataPermission(f -> f.likeIgnoreCase(StringUtils.hasText(searchDeviceRequest.getName()), DevicePO.Fields.name, searchDeviceRequest.getName()), searchDeviceRequest.toPageable())
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
    public void updateDevice(Long deviceId, UpdateDeviceRequest updateDeviceRequest) {
        Optional<DevicePO> findResult = deviceRepository.findByIdWithDataPermission(deviceId);
        if (findResult.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        DevicePO device = findResult.get();
        String newName = updateDeviceRequest.getName();
        if (device.getName().equals(newName)) {
            return;
        }

        device.setName(newName);

        deviceRepository.save(device);
        eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.UPDATED, deviceConverter.convertPO(device)));
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDevices(List<String> deviceIdList) {
        if (deviceIdList.isEmpty()) {
            return;
        }

        List<DevicePO> devicePOList = deviceRepository.findByIdInWithDataPermission(deviceIdList.stream().map(Long::valueOf).toList());
        Set<String> foundIds = devicePOList.stream().map(id -> id.getId().toString()).collect(Collectors.toSet());


        // check whether all devices exist
        if (!new HashSet<>(deviceIdList).containsAll(foundIds)) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND)
                    .detailMessage("Some id not found!")
                    .build();
        }

        List<Device> devices = deviceConverter.convertPO(devicePOList);

        Map<String, Integration> integrationMap = getIntegrationMap(devices.stream().map(Device::getIntegrationId).toList());

        devices.stream().map((Device device) -> {
            // check ability to delete device
            Integration integrationConfig = integrationMap.get(device.getIntegrationId());
            if (integrationConfig == null) {
                deleteDevice(device);
                return null;
            }

            ExchangePayload payload = new ExchangePayload();
            String deleteDeviceServiceKey = integrationConfig.getEntityKeyDeleteDevice();
            if (deleteDeviceServiceKey == null) {
                throw ServiceException
                        .with(ErrorCode.METHOD_NOT_ALLOWED)
                        .detailMessage("integration " + device.getIntegrationId() + " cannot delete device!")
                        .build();
            }

            payload.put(deleteDeviceServiceKey, "");
            payload.putContext(DEVICE_ON_DELETE, device);
            return payload;
        }).filter(Objects::nonNull).forEach((ExchangePayload payload) -> {
            // call service for deleting
            try {
                entityValueServiceProvider.saveValuesAndPublishSync(payload);
            } catch (Exception e) {
                throw ServiceException
                        .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "delete device failed")
                        .build();
            }
        });
    }

    public DeviceDetailResponse getDeviceDetail(Long deviceId) {
        Optional<DevicePO> findResult = deviceRepository.findByIdWithDataPermission(deviceId);
        if (findResult.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        DeviceDetailResponse deviceDetailResponse = new DeviceDetailResponse();
        BeanUtils.copyProperties(convertPOToResponseData(findResult.get()), deviceDetailResponse);
        fillIntegrationInfo(List.of(deviceDetailResponse));

        // set entities
        List<Entity> entities = entityServiceProvider.findByTargetId(AttachTargetType.DEVICE, deviceId.toString());
        deviceDetailResponse.setIdentifier(findResult.get().getIdentifier());
        deviceDetailResponse.setEntities(entities
                .stream().flatMap((Entity pEntity) -> {
                    ArrayList<Entity> flatEntities = new ArrayList<>();
                    flatEntities.add(pEntity);

                    List<Entity> childrenEntities = pEntity.getChildren();
                    if (childrenEntities != null) {
                        flatEntities.addAll(childrenEntities);
                    }

                    return flatEntities.stream().map(entity -> DeviceEntityData
                            .builder()
                            .id(entity.getId().toString())
                            .key(entity.getKey())
                            .type(entity.getType())
                            .name(entity.getName())
                            .valueType(entity.getValueType())
                            .valueAttribute(entity.getAttributes())
                            .description(entity.getDescription())
                            .build());
                }).toList());
        return deviceDetailResponse;
    }

    // Device API Implementations

    private List<DeviceNameDTO> convertDevicePOList(List<DevicePO> devicePOList) {
        Map<String, Integration> integrationMap = getIntegrationMap(devicePOList.stream().map(DevicePO::getIntegration).toList());
        return devicePOList.stream().map(devicePO -> DeviceNameDTO.builder()
                .id(devicePO.getId())
                .name(devicePO.getName())
                .key(devicePO.getKey())
                .userId(devicePO.getUserId())
                .createdAt(devicePO.getCreatedAt())
                .integrationId(devicePO.getIntegration())
                .integrationConfig(integrationMap.get(devicePO.getIntegration()))
                .build()
        ).toList();
    }

    @Override
    public List<DeviceNameDTO> fuzzySearchDeviceByName(String name) {
        return convertDevicePOList(deviceRepository
                .findAll(f -> f.likeIgnoreCase(DevicePO.Fields.name, name))
                .stream()
                .toList());
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByIntegrations(List<String> integrationIds) {
        if (integrationIds == null || integrationIds.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDevicePOList(deviceRepository
                .findAll(f -> f.in(DevicePO.Fields.integration, integrationIds.toArray()))
                .stream()
                .toList());
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByIds(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDevicePOList(deviceRepository.findByIdIn(deviceIds)
                .stream()
                .toList());
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByKey(List<String> deviceKeys) {
        if (deviceKeys == null || deviceKeys.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDevicePOList(deviceRepository.findAll(f -> f.in(DevicePO.Fields.key, deviceKeys.toArray()))
                .stream()
                .toList());
    }

    @Override
    public DeviceNameDTO getDeviceNameByKey(String deviceKey) {
        Optional<DevicePO> devicePO = deviceRepository.findOne(f -> f.eq(DevicePO.Fields.key, deviceKey));
        return devicePO.map(po -> convertDevicePOList(List.of(po)).get(0)).orElse(null);

    }

    @Override
    public Map<String, Long> countByIntegrationIds(List<String> integrationIds) {
        List<Object[]> res = deviceRepository.countByIntegrations(integrationIds);
        Map<String, Long> result = new HashMap<>();
        res.forEach((Object[] o) -> result.put((String) o[0], (Long) o[1]));
        return result;
    }

    @Override
    public Long countByIntegrationId(String integrationId) {
        return deviceRepository.count(f -> f.eq(DevicePO.Fields.integration, integrationId));
    }

    public void deleteDevice(Device device) {
        entityServiceProvider.deleteByTargetId(device.getId().toString());

        deviceRepository.deleteById(device.getId());

        userFacade.deleteResource(ResourceType.DEVICE, Collections.singletonList(device.getId()));
        userFacade.deleteResource(ResourceType.ENTITY, device.getEntities().stream().map(Entity::getId).collect(Collectors.toList()));

        eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.DELETED, device));
    }
}
