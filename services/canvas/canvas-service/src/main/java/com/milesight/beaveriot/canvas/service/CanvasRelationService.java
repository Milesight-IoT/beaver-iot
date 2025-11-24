package com.milesight.beaveriot.canvas.service;

import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.canvas.constants.CanvasDataFieldConstants;
import com.milesight.beaveriot.canvas.po.CanvasDevicePO;
import com.milesight.beaveriot.canvas.po.CanvasEntityPO;
import com.milesight.beaveriot.canvas.repository.CanvasDeviceRepository;
import com.milesight.beaveriot.canvas.repository.CanvasEntityRepository;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.device.dto.DeviceIdKeyDTO;
import com.milesight.beaveriot.device.dto.DeviceResponseData;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.device.facade.IDeviceResponseFacade;
import com.milesight.beaveriot.entity.dto.EntityIdKeyDTO;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CanvasRelationService class.
 *
 * @author simon
 * @date 2025/9/17
 */
@Service
public class CanvasRelationService {

    @Autowired
    CanvasEntityRepository canvasEntityRepository;

    @Autowired
    CanvasDeviceRepository canvasDeviceRepository;

    @Autowired
    IEntityFacade entityFacade;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    IDeviceFacade deviceFacade;

    @Autowired
    IDeviceResponseFacade deviceResponseFacade;

    @Data
    public static class CanvasEntityResult {
        // May include only parent entities
        List<Long> entityIdList = new ArrayList<>();

        // All canvas entities including child entities
        List<EntityResponse> entityList = new ArrayList<>();
    }

    public CanvasEntityResult getCanvasEntities(Long canvasId) {
        CanvasEntityResult canvasEntityResult = new CanvasEntityResult();
        List<CanvasEntityPO> canvasEntityList = canvasEntityRepository
                .findAll(filter -> filter.eq(CanvasEntityPO.Fields.canvasId, canvasId));
        if (!canvasEntityList.isEmpty()) {
            List<Long> entityIds = canvasEntityList.stream().map(CanvasEntityPO::getEntityId).toList();
            canvasEntityResult.setEntityIdList(entityIds);
            EntityQuery query = new EntityQuery();
            query.setEntityIds(entityIds);
            query.setPageNumber(1);
            query.setPageSize(CanvasDataFieldConstants.ENTITY_MAX_COUNT_PER_CANVAS);
            query.setSort(new Sorts().asc("id"));
            canvasEntityResult.setEntityList(entityFacade.search(query).getContent());
        }

        return canvasEntityResult;
    }

    public List<DeviceResponseData> getCanvasDevices(Long canvasId) {
        List<CanvasDevicePO> canvasDeviceList = canvasDeviceRepository
                .findAll(filter -> filter.eq(CanvasEntityPO.Fields.canvasId, canvasId));
        if (canvasDeviceList.isEmpty()) {
            return List.of();
        }

        return deviceResponseFacade
                .getDeviceResponseByIds(canvasDeviceList.stream().map(CanvasDevicePO::getDeviceId).toList())
                .getContent();
    }

    public void saveCanvasEntities(Long canvasId, List<Long> entityIdList) {
        // Handle empty or null input
        List<EntityIdKeyDTO> validatedEntities = CollectionUtils.isEmpty(entityIdList) ? List.of()
                : entityFacade.findIdAndKeyByIds(entityIdList);
        if (CollectionUtils.isEmpty(validatedEntities)) {
            // Delete all existing relations if input is empty
            canvasEntityRepository.deleteAllByCanvasId(canvasId);
            canvasEntityRepository.flush();
            return;
        }

        // Get existing relations
        List<CanvasEntityPO> existingRelations = canvasEntityRepository
                .findAll(filter -> filter.eq(CanvasEntityPO.Fields.canvasId, canvasId));

        // Extract existing entity IDs
        Set<Long> existingEntityIds = existingRelations.stream()
                .map(CanvasEntityPO::getEntityId)
                .collect(Collectors.toSet());

        // Extract validated entity IDs
        Set<Long> validatedEntityIds = validatedEntities.stream()
                .map(EntityIdKeyDTO::getId)
                .collect(Collectors.toSet());

        // To delete: existing IDs not in validated list
        List<Long> toDelete = existingEntityIds.stream()
                .filter(id -> !validatedEntityIds.contains(id))
                .toList();

        // To add: validated IDs not in existing list
        List<EntityIdKeyDTO> toAdd = validatedEntities.stream()
                .filter(entity -> !existingEntityIds.contains(entity.getId()))
                .toList();

        if (!toDelete.isEmpty()) {
            canvasEntityRepository.deleteAllByCanvasIdAndEntityIdIn(canvasId, toDelete);
            canvasEntityRepository.flush();
        }

        if (!toAdd.isEmpty()) {
            List<CanvasEntityPO> newRelations = toAdd.stream()
                    .map(entity -> CanvasEntityPO.builder()
                            .id(SnowflakeUtil.nextId())
                            .canvasId(canvasId)
                            .entityId(entity.getId())
                            .entityKey(entity.getKey())
                            .build())
                    .toList();
            canvasEntityRepository.saveAll(newRelations);
        }
    }

    public void saveCanvasDevices(Long canvasId, List<Long> deviceIdList) {
        List<DeviceIdKeyDTO> validatedDevices = CollectionUtils.isEmpty(deviceIdList) ? List.of()
                : deviceFacade.findIdAndKeyByIds(deviceIdList);

        // Handle empty or null input
        if (CollectionUtils.isEmpty(validatedDevices)) {
            // Delete all existing relations if input is empty
            canvasDeviceRepository.deleteAllByCanvasIdIn(List.of(canvasId));
            canvasDeviceRepository.flush();
            return;
        }

        // Get existing relations
        List<CanvasDevicePO> existingRelations = canvasDeviceRepository
                .findAll(filter -> filter.eq(CanvasDevicePO.Fields.canvasId, canvasId));

        // Extract existing device IDs
        Set<Long> existingDeviceIds = existingRelations.stream()
                .map(CanvasDevicePO::getDeviceId)
                .collect(Collectors.toSet());

        // Extract validated device IDs
        Set<Long> validatedDeviceIds = validatedDevices.stream()
                .map(DeviceIdKeyDTO::getId)
                .collect(Collectors.toSet());

        // To delete: existing IDs not in validated list
        List<Long> toDelete = existingDeviceIds.stream()
                .filter(id -> !validatedDeviceIds.contains(id))
                .toList();

        // To add: validated IDs not in existing list
        List<DeviceIdKeyDTO> toAdd = validatedDevices.stream()
                .filter(device -> !existingDeviceIds.contains(device.getId()))
                .toList();

        // Execute only necessary operations
        if (!toDelete.isEmpty()) {
            canvasDeviceRepository.deleteAllByCanvasIdAndDeviceIdIn(canvasId, toDelete);
            canvasDeviceRepository.flush();
        }

        if (!toAdd.isEmpty()) {
            List<CanvasDevicePO> newRelations = toAdd.stream()
                    .map(device -> CanvasDevicePO.builder()
                            .id(SnowflakeUtil.nextId())
                            .canvasId(canvasId)
                            .deviceId(device.getId())
                            .build())
                    .toList();
            canvasDeviceRepository.saveAll(newRelations);
        }
    }

    public void deleteCanvasRelations(List<Long> canvasIdList) {
        canvasEntityRepository.deleteAllByCanvasIdIn(canvasIdList);
        canvasDeviceRepository.deleteAllByCanvasIdIn(canvasIdList);
    }
}
