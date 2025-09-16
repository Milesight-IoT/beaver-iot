package com.milesight.beaveriot.canvas.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.canvas.constants.CanvasDataFieldConstants;
import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.enums.CanvasOp;
import com.milesight.beaveriot.canvas.model.dto.CanvasWidgetDTO;
import com.milesight.beaveriot.canvas.model.request.CanvasUpdateRequest;
import com.milesight.beaveriot.canvas.model.response.CanvasResponse;
import com.milesight.beaveriot.canvas.po.CanvasEntityPO;
import com.milesight.beaveriot.canvas.po.CanvasPO;
import com.milesight.beaveriot.canvas.po.CanvasWidgetPO;
import com.milesight.beaveriot.canvas.repository.CanvasEntityRepository;
import com.milesight.beaveriot.canvas.repository.CanvasRepository;
import com.milesight.beaveriot.canvas.repository.CanvasWidgetRepository;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.dashboard.dto.DashboardDTO;
import com.milesight.beaveriot.dashboard.facade.IDashboardFacade;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.permission.facade.IPermissionFacade;
import com.milesight.beaveriot.resource.manager.dto.ResourceRefDTO;
import com.milesight.beaveriot.resource.manager.enums.ResourceRefType;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CanvasService class.
 *
 * @author simon
 * @date 2025/9/9
 */
@Service
public class CanvasService {
    @Autowired
    CanvasRepository canvasRepository;

    @Autowired
    CanvasWidgetRepository canvasWidgetRepository;

    @Autowired
    IPermissionFacade permissionFacade;

    @Autowired
    IDashboardFacade dashboardFacade;

    @Autowired
    CanvasEntityRepository canvasEntityRepository;

    @Autowired
    IEntityFacade entityFacade;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    ResourceManagerFacade resourceManagerFacade;

    protected void checkCanvasPermission(CanvasPO canvasPO, CanvasOp userOp) {
        if (canvasPO.getAttachType().equals(CanvasAttachType.DASHBOARD)) {
            switch (userOp) {
                case READ -> permissionFacade.checkMenuPermission(new OperationPermissionCode[]{OperationPermissionCode.DASHBOARD_VIEW});
                case UPDATE -> permissionFacade.checkMenuPermission(new OperationPermissionCode[]{OperationPermissionCode.DASHBOARD_EDIT});
            }

            permissionFacade.checkDataPermission(DataPermissionType.DASHBOARD, canvasPO.getAttachId());
        } else if (canvasPO.getAttachType().equals(CanvasAttachType.DEVICE)) {
            switch (userOp) {
                case READ -> permissionFacade.checkMenuPermission(new OperationPermissionCode[]{OperationPermissionCode.DEVICE_VIEW});
                case UPDATE -> permissionFacade.checkMenuPermission(new OperationPermissionCode[]{OperationPermissionCode.DEVICE_EDIT});
            }

            permissionFacade.checkDataPermission(DataPermissionType.DEVICE, canvasPO.getAttachId());
        } else {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Unsupported canvas permission check: " + canvasPO.getAttachType()).build();
        }
    }

    protected CanvasPO getCanvasById(Long canvasId) {
        return canvasRepository.findById(canvasId)
                .orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("canvas not exist").build());
    }

    public CanvasResponse getCanvasData(Long canvasId) {
        CanvasPO canvasPO = getCanvasById(canvasId);
        checkCanvasPermission(canvasPO, CanvasOp.READ);
        CanvasResponse canvasResponse = new CanvasResponse();
        canvasResponse.setId(canvasPO.getId().toString());
        canvasResponse.setName(canvasPO.getName());
        canvasResponse.setAttachId(canvasPO.getAttachId());
        canvasResponse.setAttachType(canvasPO.getAttachType());

        List<CanvasWidgetPO> canvasWidgetPOList = canvasWidgetRepository.findAll(f -> f.eq(CanvasWidgetPO.Fields.canvasId, canvasId));
        canvasResponse.setWidgets(canvasWidgetPOList.stream().map(canvasWidgetPO -> {
            CanvasWidgetDTO canvasWidgetDTO = new CanvasWidgetDTO();
            canvasWidgetDTO.setWidgetId(canvasWidgetPO.getId().toString());
            canvasWidgetDTO.setData(canvasWidgetPO.getData());
            return canvasWidgetDTO;
        }).collect(Collectors.toList()));

        List<CanvasEntityPO> canvasEntityList = canvasEntityRepository.findAll(filter -> filter.eq(CanvasEntityPO.Fields.canvasId, canvasId));
        if (!canvasEntityList.isEmpty()) {
            List<Long> entityIds = canvasEntityList.stream().map(CanvasEntityPO::getEntityId).toList();
            canvasResponse.setEntityIds(entityIds.stream().map(Object::toString).toList());
            EntityQuery query = new EntityQuery();
            query.setEntityIds(entityIds);
            query.setPageNumber(1);
            query.setPageSize(CanvasDataFieldConstants.ENTITY_MAX_COUNT_PER_DASHBOARD);
            query.setSort(new Sorts().asc("id"));
            canvasResponse.setEntities(entityFacade.search(query).getContent());
        } else {
            canvasResponse.setEntityIds(List.of());
            canvasResponse.setEntities(List.of());
        }
        return canvasResponse;
    }

    public String getCanvasWidgetFileUrl(Map<String, Object> widgetData) {
        return Optional.ofNullable(widgetData)
                .map(m -> (Map<String, Object>) m.get("config"))
                .map(m -> (Map<String, Object>) m.get("file"))
                .map(m -> m.get("url"))
                .map(Object::toString)
                .orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCanvas(Long canvasId, CanvasUpdateRequest updateRequest) {
        CanvasPO canvasPO = getCanvasById(canvasId);
        checkCanvasPermission(canvasPO, CanvasOp.UPDATE);

        canvasPO.setName(updateRequest.getName());
        canvasRepository.save(canvasPO);

        canvasEntityRepository.deleteAllByCanvasId(canvasId);
        canvasEntityRepository.flush();
        if (!CollectionUtils.isEmpty(updateRequest.getEntityIds())) {
            List<Entity> entities = entityServiceProvider.findByIds(updateRequest.getEntityIds());
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

        List<CanvasWidgetDTO> canvasWidgetDTOList = updateRequest.getWidgets();
        List<CanvasWidgetPO> prevCanvasWidgetPOList = canvasWidgetRepository.findAll(filter -> filter.eq(CanvasWidgetPO.Fields.canvasId, canvasId));
        Map<String, CanvasWidgetPO> canvasWidgetPOMap = new HashMap<>();
        if (prevCanvasWidgetPOList != null && !prevCanvasWidgetPOList.isEmpty()) {
            canvasWidgetPOMap.putAll(prevCanvasWidgetPOList.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), Function.identity())));
        }
        List<CanvasWidgetPO> canvasWidgetPOList = new ArrayList<>();
        Map<String, String> deleteUrlMap = new HashMap<>();
        Map<String, String> addUrlMap = new HashMap<>();
        if (canvasWidgetDTOList != null && !canvasWidgetDTOList.isEmpty()) {
            canvasWidgetDTOList.forEach(canvasWidgetDTO -> {
                String widgetId = canvasWidgetDTO.getWidgetId();
                Map<String, Object> data = canvasWidgetDTO.getData();
                String url = getCanvasWidgetFileUrl(data);
                if (widgetId == null) {
                    // create new widgets
                    CanvasWidgetPO canvasWidgetPO = new CanvasWidgetPO();
                    canvasWidgetPO.setId(SnowflakeUtil.nextId());
                    canvasWidgetPO.setUserId(SecurityUserContext.getUserId());
                    canvasWidgetPO.setTenantId(canvasPO.getTenantId());
                    canvasWidgetPO.setCanvasId(canvasId);
                    canvasWidgetPO.setData(data);
                    canvasWidgetPOList.add(canvasWidgetPO);

                    if (StringUtils.hasText(url)) {
                        addUrlMap.put(String.valueOf(canvasWidgetPO.getId()), url);
                    }
                } else {
                    // update previous widgets
                    CanvasWidgetPO existCanvasWidgetPO = canvasWidgetPOMap.get(widgetId);
                    String prevUrl = null;
                    if (existCanvasWidgetPO != null) {
                        prevUrl = getCanvasWidgetFileUrl(existCanvasWidgetPO.getData());
                        existCanvasWidgetPO.setData(data);
                        canvasWidgetPOList.add(existCanvasWidgetPO);
                    }
                    if (StringUtils.hasText(prevUrl) && !prevUrl.equals(url)) {
                        deleteUrlMap.put(widgetId, prevUrl);
                    }
                    if (StringUtils.hasText(url) && !url.equals(prevUrl)) {
                        addUrlMap.put(widgetId, url);
                    }
                }
            });
        }
        List<Long> canvasWidgetIdList = canvasWidgetPOList.stream().map(CanvasWidgetPO::getId).toList();
        List<Long> deleteCanvasWidgetIdList = new ArrayList<>();
        if (prevCanvasWidgetPOList != null && !prevCanvasWidgetPOList.isEmpty()) {
            List<CanvasWidgetPO> deleteCanvasWidgetPOList = prevCanvasWidgetPOList.stream().filter(t -> !canvasWidgetIdList.contains(t.getId())).toList();
            deleteCanvasWidgetIdList.addAll(deleteCanvasWidgetPOList.stream().map(CanvasWidgetPO::getId).toList());
            deleteCanvasWidgetPOList.forEach(t -> {
                String deleteUrl = getCanvasWidgetFileUrl(t.getData());
                if (StringUtils.hasText(deleteUrl)) {
                    deleteUrlMap.put(String.valueOf(t.getId()), deleteUrl);
                }
            });
        }
        if (!deleteCanvasWidgetIdList.isEmpty()) {
            canvasWidgetRepository.deleteAllById(deleteCanvasWidgetIdList);
        }
        if (!canvasWidgetPOList.isEmpty()) {
            canvasWidgetRepository.saveAll(canvasWidgetPOList);
        }
        deleteUrlMap.forEach((widgetId, url) -> resourceManagerFacade.unlinkRef(new ResourceRefDTO(widgetId, ResourceRefType.DASHBOARD_WIDGET.name())));
        addUrlMap.forEach((widgetId, url) -> resourceManagerFacade.linkByUrl(url, new ResourceRefDTO(widgetId, ResourceRefType.DASHBOARD_WIDGET.name())));
    }

    @Transactional
    protected void doDeleteCanvasByIdList(List<Long> canvasIdList) {
        if (canvasIdList == null || canvasIdList.isEmpty()) {
            return;
        }

        Map<String, String> deleteUrlMap = new HashMap<>();
        canvasWidgetRepository
                .findAll(filter -> filter.in(CanvasWidgetPO.Fields.canvasId, canvasIdList.toArray()))
                .forEach(canvasWidgetPO -> {
                    String prevUrl = getCanvasWidgetFileUrl(canvasWidgetPO.getData());
                    if (StringUtils.hasText(prevUrl)) {
                        deleteUrlMap.put(String.valueOf(canvasWidgetPO.getId()), prevUrl);
                    }
                });

        deleteUrlMap.forEach((widgetId, url) -> resourceManagerFacade.unlinkRef(new ResourceRefDTO(widgetId, ResourceRefType.DASHBOARD_WIDGET.name())));
        canvasWidgetRepository.deleteByCanvasIdIn(canvasIdList);
        canvasEntityRepository.deleteAllByCanvasIdIn(canvasIdList);
        canvasRepository.deleteAllById(canvasIdList);
    }
}
