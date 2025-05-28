package com.milesight.beaveriot.entity.facade;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.entity.convert.EntityConverter;
import com.milesight.beaveriot.entity.dto.EntityDTO;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.entity.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/11/25 16:46
 */
@Service
public class EntityFacade implements IEntityFacade {

    @Autowired
    private EntityRepository entityRepository;
    @Autowired
    private EntityService entityService;
    @Autowired
    IDeviceFacade deviceFacade;
    @Autowired
    @Lazy
    DeviceServiceProvider deviceServiceProvider;

    @Override
    public Page<EntityResponse> search(EntityQuery entityQuery) {
        return entityService.search(entityQuery);
    }

    public List<EntityDTO> getUserOrTargetEntities(Long userId, List<String> targetIds) {
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.or(filter1 -> filter1.eq(EntityPO.Fields.userId, userId)
                        .in(!targetIds.isEmpty(), EntityPO.Fields.attachTargetId, targetIds.toArray())
                ));
        return EntityConverter.INSTANCE.convertDTOList(entityPOList);
    }

    public List<EntityDTO> getTargetEntities(List<String> targetIds) {
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(!targetIds.isEmpty(), EntityPO.Fields.attachTargetId, targetIds.toArray())
        );
        return EntityConverter.INSTANCE.convertDTOList(entityPOList);
    }

    /**
     * Batch delete customized entities by ids
     *
     * @param entityIds entity ids
     */
    @Override
    public void deleteCustomizedEntitiesByIds(List<Long> entityIds) {
        entityService.deleteCustomizedEntitiesByIds(entityIds);
    }

    @Override
    public long countAllEntitiesByIntegrationId(String integrationId) {
        if (!StringUtils.hasText(integrationId)) {
            return 0L;
        }
        long allEntityCount = 0L;
        long integrationEntityCount = countIntegrationEntitiesByIntegrationId(integrationId);
        allEntityCount += integrationEntityCount;
        List<Device> integrationDevices = deviceServiceProvider.findAll(integrationId);
        if (integrationDevices != null && !integrationDevices.isEmpty()) {
            List<String> deviceIds = integrationDevices.stream().map(t -> String.valueOf(t.getId())).toList();
            List<EntityPO> deviceEntityPOList = entityRepository.findAll(filter -> filter.eq(EntityPO.Fields.attachTarget, AttachTargetType.DEVICE).in(EntityPO.Fields.attachTargetId, deviceIds.toArray()));
            if (deviceEntityPOList != null && !deviceEntityPOList.isEmpty()) {
                allEntityCount += deviceEntityPOList.size();
            }
        }
        return allEntityCount;
    }

    @Override
    public Map<String, Long> countAllEntitiesByIntegrationIds(List<String> integrationIds) {
        if (integrationIds == null || integrationIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Long> allEntityCountMap = new HashMap<>();
        allEntityCountMap.putAll(countIntegrationEntitiesByIntegrationIds(integrationIds));
        List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
        if (integrationDevices != null && !integrationDevices.isEmpty()) {
            Map<String, List<DeviceNameDTO>> integrationDeviceMap = integrationDevices.stream().filter(t -> t.getIntegrationConfig() != null).collect(Collectors.groupingBy(t -> t.getIntegrationConfig().getId()));
            List<String> deviceIds = integrationDevices.stream().map(DeviceNameDTO::getId).map(String::valueOf).toList();
            List<EntityPO> deviceEntityPOList = entityRepository.findAll(filter -> filter.eq(EntityPO.Fields.attachTarget, AttachTargetType.DEVICE).in(EntityPO.Fields.attachTargetId, deviceIds.toArray()));
            if (deviceEntityPOList != null && !deviceEntityPOList.isEmpty()) {
                Map<String, Long> deviceEntityCountMap = deviceEntityPOList.stream().collect(Collectors.groupingBy(EntityPO::getAttachTargetId, Collectors.counting()));
                integrationDeviceMap.forEach((integrationId, deviceList) -> {
                    if (deviceList == null || deviceList.isEmpty()) {
                        return;
                    }
                    List<String> deviceIdList = deviceList.stream().map(DeviceNameDTO::getId).map(String::valueOf).toList();
                    Long integrationDeviceCount = 0L;
                    for (String deviceId : deviceIdList) {
                        Long deviceCount = deviceEntityCountMap.get(deviceId);
                        if (deviceCount != null) {
                            integrationDeviceCount += deviceCount;
                        }
                    }
                    Long entityCount = allEntityCountMap.get(integrationId) == null ? 0L : allEntityCountMap.get(integrationId);
                    allEntityCountMap.put(integrationId, entityCount + integrationDeviceCount);
                });
            }
        }
        return allEntityCountMap;
    }

    @Override
    public long countIntegrationEntitiesByIntegrationId(String integrationId) {
        if (!StringUtils.hasText(integrationId)) {
            return 0L;
        }
        List<EntityPO> integrationEntityPOList = entityRepository.findAll(filter -> filter.eq(EntityPO.Fields.attachTarget, AttachTargetType.INTEGRATION).eq(EntityPO.Fields.attachTargetId, integrationId));
        if (integrationEntityPOList == null || integrationEntityPOList.isEmpty()) {
            return 0L;
        }
        return integrationEntityPOList.size();
    }

    @Override
    public Map<String, Long> countIntegrationEntitiesByIntegrationIds(List<String> integrationIds) {
        if (integrationIds == null || integrationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<EntityPO> integrationEntityPOList = entityRepository.findAll(filter -> filter.eq(EntityPO.Fields.attachTarget, AttachTargetType.INTEGRATION).in(EntityPO.Fields.attachTargetId, integrationIds.toArray()));
        if (integrationEntityPOList == null || integrationEntityPOList.isEmpty()) {
            return Collections.emptyMap();
        }
        return integrationEntityPOList.stream().collect(Collectors.groupingBy(EntityPO::getAttachTargetId, Collectors.counting()));
    }

}
