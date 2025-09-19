package com.milesight.beaveriot.canvas.service;

import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.canvas.constants.CanvasDataFieldConstants;
import com.milesight.beaveriot.canvas.po.CanvasDevicePO;
import com.milesight.beaveriot.canvas.po.CanvasEntityPO;
import com.milesight.beaveriot.canvas.repository.CanvasDeviceRepository;
import com.milesight.beaveriot.canvas.repository.CanvasEntityRepository;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.dto.DeviceResponseData;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.device.facade.IDeviceResponseFacade;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

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
        List<CanvasEntityPO> canvasEntityList = canvasEntityRepository.findAll(filter -> filter.eq(CanvasEntityPO.Fields.canvasId, canvasId));
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
        List<CanvasDevicePO> canvasDeviceList = canvasDeviceRepository.findAll(filter -> filter.eq(CanvasEntityPO.Fields.canvasId, canvasId));
        if (canvasDeviceList.isEmpty()) {
            return List.of();
        }

        return deviceResponseFacade.getDeviceResponseByIds(canvasDeviceList.stream().map(CanvasDevicePO::getDeviceId).toList()).getContent();
    }

    public void saveCanvasEntities(Long canvasId, List<Long> entityIdList) {
        canvasEntityRepository.deleteAllByCanvasId(canvasId);
        canvasEntityRepository.flush();

        if (CollectionUtils.isEmpty(entityIdList)) {
            return;
        }

        List<Entity> entities = entityServiceProvider.findByIds(entityIdList);
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }

        List<CanvasEntityPO> canvasEntities = entities.stream()
                .map(e -> CanvasEntityPO.builder()
                        .id(SnowflakeUtil.nextId())
                        .canvasId(canvasId)
                        .entityId(e.getId())
                        .entityKey(e.getKey())
                        .build())
                .toList();
        canvasEntityRepository.saveAll(canvasEntities);
    }

    public void saveCanvasDevices(Long canvasId, List<Long> deviceIdList) {
        canvasDeviceRepository.deleteAllByCanvasIdIn(List.of(canvasId));
        canvasDeviceRepository.flush();
        if (CollectionUtils.isEmpty(deviceIdList)) {
            return;
        }

        List<DeviceNameDTO> devices = deviceFacade.getDeviceNameByIds(deviceIdList);
        if (CollectionUtils.isEmpty(devices)) {
            return;
        }

        List<CanvasDevicePO> canvasDevices = devices.stream()
                .map(e -> CanvasDevicePO.builder()
                        .id(SnowflakeUtil.nextId())
                        .canvasId(canvasId)
                        .deviceId(e.getId())
                        .build())
                .toList();
        canvasDeviceRepository.saveAll(canvasDevices);
    }

    public void deleteCanvasRelations(List<Long> canvasIdList) {
        canvasEntityRepository.deleteAllByCanvasIdIn(canvasIdList);
        canvasDeviceRepository.deleteAllByCanvasIdIn(canvasIdList);
    }
}
