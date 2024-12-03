package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
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

@Service
@Slf4j
public class DeviceService implements IDeviceFacade {
    @Autowired
    DeviceRepository deviceRepository;

    @Lazy
    @Autowired
    IntegrationServiceProvider integrationServiceProvider;

    @Autowired
    ExchangeFlowExecutor exchangeFlowExecutor;

    @Lazy
    @Autowired
    DeviceConverter deviceConverter;

    @Lazy
    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    EventBus eventBus;


    private Optional<Integration> getIntegrationConfig(String integrationIdentifier) {
        return Optional.ofNullable(integrationServiceProvider.getIntegration(integrationIdentifier));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDevice(CreateDeviceRequest createDeviceRequest) {
        String integrationIdentifier = createDeviceRequest.getIntegration();
        Optional<Integration> integrationConfig = getIntegrationConfig(integrationIdentifier);

        if (integrationConfig.isEmpty()) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "integration " + integrationIdentifier + " not found!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // check ability to add device
        String addDeviceEntityId = integrationConfig.get().getEntityIdentifierAddDevice();
        if (addDeviceEntityId == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "integration " + integrationIdentifier + " cannot add device!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // call service for adding
        ExchangePayload payload = createDeviceRequest.getParamEntities();
        payload.putContext("device_name", createDeviceRequest.getName());

        // Must return a device
        try {
            exchangeFlowExecutor.syncExchangeDown(payload);
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

        Optional<Integration> integrationConfig = getIntegrationConfig(devicePO.getIntegration());
        if (integrationConfig.isPresent()) {
            deviceResponseData.setDeletable(integrationConfig.get().getEntityIdentifierDeleteDevice() != null);
            deviceResponseData.setIntegrationName(integrationConfig.get().getName());
        } else {
            deviceResponseData.setDeletable(true);
        }

        return deviceResponseData;
    }

    public Page<DeviceResponseData> searchDevice(SearchDeviceRequest searchDeviceRequest) {
        // convert to `DeviceResponseData`
        if (searchDeviceRequest.getSort().getOrders().isEmpty()) {
            searchDeviceRequest.sort(new Sorts().desc(DevicePO.Fields.createdAt));
        }

        return deviceRepository
                .findAll(f -> f.like(StringUtils.hasText(searchDeviceRequest.getName()), DevicePO.Fields.name, searchDeviceRequest.getName()), searchDeviceRequest.toPageable())
                .map(this::convertPOToResponseData);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(Long deviceId, UpdateDeviceRequest updateDeviceRequest) {
        Optional<DevicePO> findResult = deviceRepository.findById(deviceId);
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

        List<DevicePO> devicePOList = deviceRepository.findByIdIn(deviceIdList.stream().map(Long::valueOf).toList());
        Set<String> foundIds = devicePOList.stream().map(id -> id.getId().toString()).collect(Collectors.toSet());

        // check whether all devices exist
        if (!new HashSet<>(deviceIdList).containsAll(foundIds)) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND)
                    .detailMessage("Some id not found!")
                    .build();
        }

        List<Device> devices = deviceConverter.convertPO(devicePOList);

        devices.stream().map((Device device) -> {
            // check ability to delete device
            Optional<Integration> integrationConfig = getIntegrationConfig(device.getIntegrationId());
            if (integrationConfig.isEmpty()) {
                deleteDevice(device);
                return null;
            }

            ExchangePayload payload = new ExchangePayload();
            String deleteDeviceServiceKey = integrationConfig.get().getEntityKeyDeleteDevice();
            if (deleteDeviceServiceKey == null) {
                throw ServiceException
                        .with(ErrorCode.METHOD_NOT_ALLOWED)
                        .detailMessage("integration " + device.getIntegrationId() + " cannot delete device!")
                        .build();
            }

            payload.put(deleteDeviceServiceKey, "");
            payload.putContext("device", device);
            return payload;
        }).filter(Objects::nonNull).forEach((ExchangePayload payload) -> {
            // call service for deleting
            try {
                exchangeFlowExecutor.syncExchangeDown(payload);
            } catch (Exception e) {
                throw ServiceException
                        .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "delete device failed")
                        .build();
            }
        });
    }

    public DeviceDetailResponse getDeviceDetail(Long deviceId) {
        Optional<DevicePO> findResult = deviceRepository.findById(deviceId);
        if (findResult.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        DeviceDetailResponse deviceDetailResponse = new DeviceDetailResponse();
        BeanUtils.copyProperties(convertPOToResponseData(findResult.get()), deviceDetailResponse);

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
                            .build());
                }).toList());
        return deviceDetailResponse;
    }

    // Device API Implementations

    private DeviceNameDTO convertDevicePO(DevicePO devicePO) {
        return DeviceNameDTO.builder()
                .id(devicePO.getId())
                .name(devicePO.getName())
                .key(devicePO.getKey())
                .integrationConfig(integrationServiceProvider.getIntegration(devicePO.getIntegration()))
                .build();
    }

    @Override
    public List<DeviceNameDTO> fuzzySearchDeviceByName(String name) {
        return deviceRepository
                .findAll(f -> f.likeIgnoreCase(DevicePO.Fields.name, name))
                .stream()
                .map(this::convertDevicePO)
                .toList();
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByIntegrations(List<String> integrationIds) {
        if (integrationIds == null || integrationIds.isEmpty()) {
            return new ArrayList<>();
        }
        return deviceRepository
                .findAll(f -> f.in(DevicePO.Fields.integration, integrationIds.toArray()))
                .stream()
                .map(this::convertDevicePO)
                .toList();
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByIds(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new ArrayList<>();
        }
        return deviceRepository.findByIdIn(deviceIds)
                .stream()
                .map(this::convertDevicePO)
                .toList();
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByKey(List<String> deviceKeys) {
        if (deviceKeys == null || deviceKeys.isEmpty()) {
            return new ArrayList<>();
        }
        return deviceRepository.findAll(f -> f.in(DevicePO.Fields.key, deviceKeys.toArray()))
                .stream()
                .map(this::convertDevicePO)
                .toList();
    }

    @Override
    public DeviceNameDTO getDeviceNameByKey(String deviceKey) {
        return deviceRepository.findOne(f -> f.eq(DevicePO.Fields.key, deviceKey))
                .map(this::convertDevicePO)
                .orElse(null);
    }

    public void deleteDevice(Device device) {
        entityServiceProvider.deleteByTargetId(device.getId().toString());

        deviceRepository.deleteById(device.getId());

        eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.DELETED, device));
    }
}
