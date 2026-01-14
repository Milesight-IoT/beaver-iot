package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceLocation;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import com.milesight.beaveriot.device.location.model.DeviceLocationSetting;
import com.milesight.beaveriot.device.location.service.DeviceLocationService;
import com.milesight.beaveriot.device.po.DevicePO;
import com.milesight.beaveriot.device.repository.DeviceRepository;
import com.milesight.beaveriot.device.support.CommonDeviceAssembler;
import com.milesight.beaveriot.device.support.DeviceConverter;
import com.milesight.beaveriot.eventbus.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeviceServiceProviderImpl implements DeviceServiceProvider {
    @Autowired
    DeviceRepository deviceRepository;

    @Autowired
    DeviceConverter deviceConverter;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    EventBus eventBus;

    @Autowired
    DeviceService deviceService;

    @Autowired
    DeviceGroupService deviceGroupService;

    @Autowired
    CommonDeviceAssembler commonDeviceAssembler;

    @Lazy
    @Autowired
    DeviceStatusServiceProvider deviceStatusServiceProvider;

    @Autowired
    private DeviceLocationService deviceLocationService;

    @Override
    public void save(Device device) {
        Long userId = SecurityUserContext.getUserId();

        DevicePO devicePO;
        Assert.notNull(device.getName(), "Device Name must be provided!");
        Assert.notNull(device.getIdentifier(), "Device identifier must be provided!");
        Assert.notNull(device.getIntegrationId(), "Integration must be provided!");

        if (device.getName().length() > DeviceDataFieldConstants.DEVICE_NAME_MAX_LENGTH) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Device name over-length " + DeviceDataFieldConstants.DEVICE_NAME_MAX_LENGTH)
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        boolean shouldCreate = false;
        boolean shouldUpdate = false;

        // check id
        if (device.getId() != null) {
            devicePO = deviceRepository.findById(device.getId()).orElse(null);
            if (devicePO == null) {
                devicePO = new DevicePO();
                devicePO.setId(device.getId());
                shouldCreate = true;
            }
        } else {
            devicePO = deviceRepository
                    .findOne(f -> f
                            .eq(DevicePO.Fields.identifier, device.getIdentifier())
                            .eq(DevicePO.Fields.integration, device.getIntegrationId())
                    ).orElse(null);
            if (devicePO == null) {
                devicePO = new DevicePO();
                devicePO.setId(SnowflakeUtil.nextId());
                shouldCreate = true;
            }
        }

        // set device data
        if (!device.getName().equals(devicePO.getName())) {
            devicePO.setName(device.getName());
            shouldUpdate = true;
        }

        if (!deviceAdditionalDataEqual(device.getAdditional(), devicePO.getAdditionalData())) {
            devicePO.setAdditionalData(device.getAdditional());
            shouldUpdate = true;
        }

        if (!Objects.equals(device.getTemplate(), devicePO.getTemplate())) {
            devicePO.setTemplate(device.getTemplate());
            shouldUpdate = true;
        }

        // create or update
        if (shouldCreate) {
            devicePO.setUserId(userId);
            // integration / identifier / key would not be updated
            devicePO.setIntegration(device.getIntegrationId());
            devicePO.setIdentifier(device.getIdentifier());
            devicePO.setTemplate(device.getTemplate());
            devicePO.setKey(device.getKey());
            devicePO = deviceRepository.save(devicePO);
            eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.CREATED, device));
        } else if (shouldUpdate) {
            devicePO = deviceRepository.save(devicePO);
            eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.UPDATED, device));
        }

        device.setId(devicePO.getId());

        // assemble device with common entities
        commonDeviceAssembler.assemble(device);

        entityServiceProvider.batchSave(device.getEntities());

        deviceService.evictIntegrationIdToDeviceCache(device.getIntegrationId());
        Long deviceGroupId = (Long) TenantContext.tryGetTenantParam(DeviceService.TENANT_PARAM_DEVICE_GROUP_ID).orElse(null);
        if (deviceGroupId != null) {
            deviceGroupService.moveDevicesToGroupId(deviceGroupId, List.of(devicePO.getId()));
        }

        DeviceLocationSetting locationSetting = (DeviceLocationSetting) TenantContext.tryGetTenantParam(DeviceService.TENANT_PARAM_DEVICE_LOCATION).orElse(null);
        if (locationSetting != null) {
            DeviceLocation location = locationSetting.buildLocation();
            deviceLocationService.setLocation(device, location);
        }
    }

    @Override
    public void deleteById(Long id) {
        Device device = findById(id);
        Assert.notNull(device, "Delete failed. Cannot find device " + id.toString());
        deviceService.deleteDevice(device);
    }

    @Override
    public Device findById(Long id) {
        return deviceService.findById(id);
    }

    @Override
    public Device findByKey(String deviceKey) {
        return deviceRepository
                .findOne(f -> f
                        .eq(DevicePO.Fields.key, deviceKey)
                )
                .map(deviceConverter::convertPO)
                .orElse(null);
    }

    @Override
    public List<Device> findByKeys(List<String> deviceKeys) {
        if (ObjectUtils.isEmpty(deviceKeys)) {
            return List.of();
        }

        return deviceConverter.convertPO(deviceRepository
                .findAll(f -> f
                        .in(DevicePO.Fields.key, deviceKeys.toArray())
                ));
    }

    @Override
    public Device findByIdentifier(String identifier, String integrationId) {
        return deviceRepository
                .findOne(f -> f
                        .eq(DevicePO.Fields.identifier, identifier)
                        .eq(DevicePO.Fields.integration, integrationId)
                )
                .map(deviceConverter::convertPO)
                .orElse(null);
    }

    @Override
    public List<Device> findByIdentifiers(List<String> identifiers, String integrationId) {
        if (ObjectUtils.isEmpty(identifiers)) {
            return List.of();
        }

        return deviceConverter.convertPO(deviceRepository
                .findAll(f -> f
                        .in(DevicePO.Fields.identifier, identifiers.toArray())
                        .eq(DevicePO.Fields.integration, integrationId)
                ));
    }

    @Override
    public List<Device> findAll(String integrationId) {
        return deviceConverter.convertPO(deviceRepository
                .findAll(f -> f.eq(DevicePO.Fields.integration, integrationId)));
    }

    private boolean deviceAdditionalDataEqual(Map<String, Object> arg1, Map<String, Object> arg2) {
        if (arg1 == null && arg2 == null) {
            return true;
        }

        if (arg1 == null || arg2 == null) {
            return false;
        }

        return arg1.equals(arg2);
    }

    @Override
    public long countByDeviceTemplateKey(String deviceTemplateKey) {
        return deviceRepository.count(f -> f.eq(DevicePO.Fields.template, deviceTemplateKey));
    }

    @Override
    public void deleteByDeviceTemplateKey(String deviceTemplateKey) {
        List<DevicePO> devicePOs = deviceRepository.findAllByTemplate(deviceTemplateKey);
        if (CollectionUtils.isEmpty(devicePOs)) {
            return;
        }

        List<Device> devices = deviceConverter.convertPO(devicePOs);
        devices.forEach(device -> {
            deviceService.deleteDevice(device);
        });
    }

    @Override
    public void clearTemplate(String deviceTemplateKey) {
        List<DevicePO> devices = deviceRepository.findAll(f -> f.eq(DevicePO.Fields.template, deviceTemplateKey));
        devices.forEach(devicePO -> devicePO.setTemplate(null));
        deviceRepository.saveAll(devices);

        Set<String> integrationIds = devices.stream().map(DevicePO::getIntegration).collect(Collectors.toSet());
        deviceService.evictIntegrationIdToDeviceCache(integrationIds);
    }

    @Override
    public boolean existsById(Long id) {
        return deviceRepository.existsById(id);
    }
}
