package com.milesight.beaveriot.entity.facade;

import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable;
import com.milesight.beaveriot.base.annotations.cacheable.CacheKeys;
import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.entity.convert.EntityConverter;
import com.milesight.beaveriot.entity.dto.EntityDTO;
import com.milesight.beaveriot.entity.dto.EntityIdKeyDTO;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.entity.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.context.constants.CacheKeyConstants.TENANT_PREFIX;

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

    @Override
    public Page<EntityResponse> search(EntityQuery entityQuery) {
        return entityService.search(entityQuery);
    }

    public List<EntityDTO> getUserOrTargetEntities(Long userId, List<String> targetIds) {
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.or(
                filter1 -> filter1.eq(EntityPO.Fields.userId, userId)
                        .in(!targetIds.isEmpty(), EntityPO.Fields.attachTargetId, targetIds.toArray())));
        return EntityConverter.INSTANCE.convertDTOList(entityPOList);
    }

    public List<EntityDTO> getTargetEntities(List<String> targetIds) {
        List<EntityPO> entityPOList = entityRepository.findAll(
                filter -> filter.in(!targetIds.isEmpty(), EntityPO.Fields.attachTargetId, targetIds.toArray()));
        return EntityConverter.INSTANCE.convertDTOList(entityPOList);
    }

    @Override
    @BatchCacheable(cacheNames = CacheKeyConstants.ENTITY_ID_TO_KEY, keyPrefix = TENANT_PREFIX)
    public Map<Long, String> mapEntityIdToAttachTargetId(@CacheKeys Collection<Long> entityIds) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return Collections.emptyMap();
        }
        List<EntityPO> entityPOList = entityRepository
                .findAll(filter -> filter.in(EntityPO.Fields.id, entityIds.toArray()));
        return entityPOList.stream()
                .collect(Collectors.toMap(EntityPO::getId, EntityPO::getAttachTargetId, (a, b) -> a));
    }

    /**
     * Batch delete entities by ids
     *
     * @param entityIds entity ids
     */
    @Override
    public void deleteEntitiesByIds(List<Long> entityIds) {
        if (entityIds != null && !entityIds.isEmpty()) {
            entityService.deleteEntitiesByPOList(entityService.findEntityPOListAndTheirChildrenByIds(entityIds));
        }
    }

    @Override
    public long countAllEntitiesByIntegrationId(String integrationId) {
        if (!StringUtils.hasText(integrationId)) {
            return 0L;
        }

        return Optional.ofNullable(countAllEntitiesByIntegrationIds(List.of(integrationId)).get(integrationId))
                .orElse(0L);
    }

    @Override
    public Map<String, Long> countAllEntitiesByIntegrationIds(List<String> integrationIds) {
        if (CollectionUtils.isEmpty(integrationIds)) {
            return new HashMap<>();
        }

        Map<String, Long> allEntityCountMap = entityService.countEntityByTarget(AttachTargetType.INTEGRATION,
                integrationIds);
        List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
        if (CollectionUtils.isEmpty(integrationDevices)) {
            return allEntityCountMap;
        }

        Map<String, List<DeviceNameDTO>> integrationDeviceMap = integrationDevices.stream()
                .filter(DeviceNameDTO::isIntegrationExists)
                .collect(Collectors.groupingBy(DeviceNameDTO::getIntegrationId));
        if (integrationDeviceMap.isEmpty()) {
            return allEntityCountMap;
        }

        List<String> deviceIds = integrationDevices.stream()
                .map(DeviceNameDTO::getId)
                .map(String::valueOf)
                .toList();
        Map<String, Long> deviceEntityCountMap = entityService.countEntityByTarget(AttachTargetType.DEVICE, deviceIds);

        integrationDeviceMap.forEach((integrationId, deviceList) -> {
            Long entityCount = allEntityCountMap.getOrDefault(integrationId, 0L);
            long entityAndIntegrationDeviceTotalCount = deviceList.stream()
                    .map(DeviceNameDTO::getId)
                    .map(String::valueOf)
                    .map(deviceEntityCountMap::get)
                    .filter(Objects::nonNull)
                    .reduce(entityCount, Long::sum);
            allEntityCountMap.put(integrationId, entityAndIntegrationDeviceTotalCount);
        });

        return allEntityCountMap;
    }

    @Override
    public List<EntityIdKeyDTO> findIdAndKeyByIds(List<Long> entityIds) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return new ArrayList<>();
        }
        return entityRepository.findIdAndKeyByIdIn(entityIds);
    }
}
