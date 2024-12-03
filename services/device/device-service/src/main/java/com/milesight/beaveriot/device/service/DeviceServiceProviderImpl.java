package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.device.po.DevicePO;
import com.milesight.beaveriot.device.repository.DeviceRepository;
import com.milesight.beaveriot.device.support.DeviceConverter;
import com.milesight.beaveriot.eventbus.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public void save(Device device) {
        DevicePO devicePO;
        Assert.notNull(device.getName(), "Device Name must be provided!");
        Assert.notNull(device.getIdentifier(), "Device identifier must be provided!");
        Assert.notNull(device.getIntegrationId(), "Integration must be provided!");

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

        // create or update
        if (shouldCreate) {
            // integration / identifier / key would not be updated
            devicePO.setIntegration(device.getIntegrationId());
            devicePO.setIdentifier(device.getIdentifier());
            devicePO.setKey(device.getKey());
            devicePO = deviceRepository.save(devicePO);
            eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.CREATED, device));
        } else if (shouldUpdate) {
            devicePO = deviceRepository.save(devicePO);
            eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.UPDATED, device));
        }

        device.setId(devicePO.getId());

        entityServiceProvider.batchSave(device.getEntities());
    }

    @Override
    public void deleteById(Long id) {
        Device device = findById(id);
        Assert.notNull(device, "Delete failed. Cannot find device " + id.toString());
        deviceService.deleteDevice(device);
    }

    @Override
    public Device findById(Long id) {
        return deviceRepository
                .findOne(f -> f
                        .eq(DevicePO.Fields.id, id)
                )
                .map(deviceConverter::convertPO)
                .orElse(null);
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
    public List<Device> findAll(String integrationId) {
        return deviceRepository
                .findAll(f -> f.eq(DevicePO.Fields.integration, integrationId))
                .stream().map(deviceConverter::convertPO)
                .toList();
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

    private boolean deviceAdditionalDataEqual(Map<String, Object> arg1, Map<String, Object> arg2) {
        if (arg1 == null && arg2 == null) {
            return true;
        }

        if (arg1 == null || arg2 == null) {
            return false;
        }

        return arg1.equals(arg2);
    }
}
