package com.milesight.beaveriot.entity.controller;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.model.request.*;
import com.milesight.beaveriot.entity.model.response.EntityAggregateResponse;
import com.milesight.beaveriot.entity.model.response.EntityHistoryResponse;
import com.milesight.beaveriot.entity.model.response.EntityLatestResponse;
import com.milesight.beaveriot.entity.model.response.EntityMetaResponse;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.service.EntityExportService;
import com.milesight.beaveriot.entity.service.EntityService;
import com.milesight.beaveriot.entity.service.EntityValueService;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/10/16 14:21
 */
@RestController
@RequestMapping("/entity")
public class EntityController {

    @Autowired
    EntityService entityService;
    @Autowired
    EntityValueService entityValueService;
    @Autowired
    EntityExportService entityExportService;

    @OperationPermission(codes = {
            OperationPermissionCode.DASHBOARD_EDIT,
            OperationPermissionCode.ENTITY_CUSTOM_VIEW,
            OperationPermissionCode.ENTITY_DATA_VIEW,
            OperationPermissionCode.WORKFLOW_ADD,
            OperationPermissionCode.WORKFLOW_EDIT,
            OperationPermissionCode.DEVICE_VIEW
    })
    @PostMapping("/advanced-search")
    public ResponseBody<Page<EntityResponse>> advancedSearch(@RequestBody EntityAdvancedSearchQuery query) {
        return getResponseOrEmpty(() -> ResponseBuilder.success(entityService.advancedSearch(query)),
                () -> ResponseBuilder.success(Page.empty()));
    }

    @OperationPermission(codes = {
            OperationPermissionCode.DASHBOARD_EDIT,
            OperationPermissionCode.ENTITY_CUSTOM_VIEW,
            OperationPermissionCode.ENTITY_DATA_VIEW,
            OperationPermissionCode.WORKFLOW_ADD,
            OperationPermissionCode.WORKFLOW_EDIT,
            OperationPermissionCode.DEVICE_VIEW
    })
    @PostMapping("/search")
    public ResponseBody<Page<EntityResponse>> search(@RequestBody EntityQuery entityQuery) {
        return getResponseOrEmpty(() -> ResponseBuilder.success(entityService.search(entityQuery)),
                () -> ResponseBuilder.success(Page.empty()));
    }

    @OperationPermission(codes = {OperationPermissionCode.DASHBOARD_EDIT, OperationPermissionCode.DASHBOARD_VIEW, OperationPermissionCode.INTEGRATION_VIEW, OperationPermissionCode.WORKFLOW_ADD, OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.DEVICE_VIEW})
    @GetMapping("/{entityId}/children")
    public ResponseBody<List<EntityResponse>> getChildren(@PathVariable("entityId") Long entityId) {
        List<EntityResponse> entityResponse = entityService.getChildren(entityId);
        return ResponseBuilder.success(entityResponse);
    }

    @OperationPermission(codes = {
            OperationPermissionCode.DASHBOARD_EDIT,
            OperationPermissionCode.DASHBOARD_VIEW,
            OperationPermissionCode.ENTITY_DATA_VIEW,
            OperationPermissionCode.WORKFLOW_ADD,
            OperationPermissionCode.WORKFLOW_EDIT,
            OperationPermissionCode.DEVICE_VIEW
    })
    @PostMapping("/history/search")
    public ResponseBody<Page<EntityHistoryResponse>> historySearch(@RequestBody EntityHistoryQuery query) {
        return getResponseOrEmpty(() -> {
                    List<Long> entityIds = query.getEntityIds() == null
                            ? new ArrayList<>()
                            : query.getEntityIds()
                            .stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    if (query.getEntityId() != null) {
                        entityIds.add(query.getEntityId());
                    }

                    if (entityIds.isEmpty()) {
                        return ResponseBuilder.success(Page.empty());
                    }

                    List<Long> entityIdsWithPermission = entityService.listEntityPOById(entityIds)
                            .stream().map(EntityPO::getId).toList();

                    if (entityIdsWithPermission.isEmpty()) {
                        return ResponseBuilder.success(Page.empty());
                    }

                    return ResponseBuilder.success(entityValueService.historySearch(entityIdsWithPermission, query.getStartTimestamp(), query.getEndTimestamp(), query));
                },
                () -> ResponseBuilder.success(Page.empty()));
    }

    @OperationPermission(codes = {OperationPermissionCode.DASHBOARD_EDIT, OperationPermissionCode.DASHBOARD_VIEW, OperationPermissionCode.WORKFLOW_ADD, OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.DEVICE_VIEW})
    @PostMapping("/history/aggregate")
    public ResponseBody<EntityAggregateResponse> historyAggregate(@RequestBody EntityAggregateQuery entityAggregateQuery) {
        return getResponseOrEmpty(() -> ResponseBuilder.success(entityValueService.historyAggregate(entityAggregateQuery)),
                () -> ResponseBuilder.success(null));
    }

    @OperationPermission(codes = {OperationPermissionCode.DASHBOARD_EDIT, OperationPermissionCode.DASHBOARD_VIEW, OperationPermissionCode.WORKFLOW_ADD, OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.DEVICE_VIEW})
    @GetMapping("/{entityId}/status")
    public ResponseBody<EntityLatestResponse> getEntityStatus(@PathVariable("entityId") Long entityId) {
        return getResponseOrEmpty(() -> ResponseBuilder.success(entityValueService.getEntityStatus(entityId)),
                () -> ResponseBuilder.success(null));
    }

    @OperationPermission(codes = {OperationPermissionCode.DASHBOARD_EDIT, OperationPermissionCode.DASHBOARD_VIEW, OperationPermissionCode.WORKFLOW_ADD, OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.DEVICE_VIEW})
    @PostMapping("/batch-get-status")
    public ResponseBody<Map<String, EntityLatestResponse>> batchGetEntityStatus(@RequestBody @Valid EntityStatusBatchGetRequest entityStatusBatchGetRequest) {
        return getResponseOrEmpty(() ->  ResponseBuilder.success(entityValueService.batchGetEntityStatus(entityStatusBatchGetRequest.getEntityIds())),
                () -> ResponseBuilder.success(Map.of()));
    }

    @GetMapping("/{entityId}/meta")
    public ResponseBody<EntityMetaResponse> getEntityMeta(@PathVariable("entityId") Long entityId) {
        EntityMetaResponse entityMetaResponse = entityService.getEntityMeta(entityId);
        return ResponseBuilder.success(entityMetaResponse);
    }

    @OperationPermission(codes = {OperationPermissionCode.INTEGRATION_VIEW,OperationPermissionCode.DASHBOARD_VIEW,OperationPermissionCode.DASHBOARD_EDIT, OperationPermissionCode.WORKFLOW_ADD, OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.DEVICE_VIEW})
    @PostMapping("/property/update")
    public ResponseBody<Void> updatePropertyEntity(@RequestBody UpdatePropertyEntityRequest updatePropertyEntityRequest) {
        entityService.updatePropertyEntity(updatePropertyEntityRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.INTEGRATION_VIEW,OperationPermissionCode.DASHBOARD_VIEW,OperationPermissionCode.DASHBOARD_EDIT, OperationPermissionCode.WORKFLOW_ADD, OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.DEVICE_VIEW})
    @PostMapping("/service/call")
    public ResponseBody<EventResponse> serviceCall(@RequestBody ServiceCallRequest serviceCallRequest) {
        EventResponse eventResponse = entityService.serviceCall(serviceCallRequest);
        return ResponseBuilder.success(eventResponse);
    }

    /**
     * Create entity
     * @param entityCreateRequest request body
     * @return created entity's metadata
     */
    @OperationPermission(codes = {OperationPermissionCode.ENTITY_CUSTOM_ADD})
    @PostMapping
    public ResponseBody<Void> createCustomEntity(@RequestBody @Valid EntityCreateRequest entityCreateRequest) {
        entityService.createCustomEntity(entityCreateRequest);
        return ResponseBuilder.success();
    }

    /**
     * Update customized entity
     * @param entityId entity ID
     * @param entityModifyRequest request body
     * @return updated entity's metadata
     */
    @OperationPermission(codes = {OperationPermissionCode.ENTITY_DATA_EDIT, OperationPermissionCode.ENTITY_CUSTOM_EDIT})
    @PutMapping("/{entityId}")
    public ResponseBody<Void> update(@PathVariable("entityId") Long entityId, @RequestBody @Valid EntityModifyRequest entityModifyRequest) {
        entityService.updateEntityBasicInfo(entityId, entityModifyRequest);
        return ResponseBuilder.success();
    }

    /**
     * Delete customized entity and its children
     * @param entityDeleteRequest request body
     */
    @OperationPermission(codes = {OperationPermissionCode.ENTITY_CUSTOM_DELETE})
    @PostMapping("/delete")
    public ResponseBody<Void> delete(@RequestBody EntityDeleteRequest entityDeleteRequest) {
        entityService.deleteCustomizedEntitiesByIds(entityDeleteRequest.getEntityIds());
        return ResponseBuilder.success();
    }

    /**
     * Export entity data as a CSV file
     * @param entityExportRequest request body
     */
    @OperationPermission(codes = {OperationPermissionCode.ENTITY_DATA_VIEW, OperationPermissionCode.DEVICE_VIEW})
    @GetMapping("/export")
    public void export(EntityExportRequest entityExportRequest, HttpServletResponse httpServletResponse) throws IOException {
        entityExportService.export(entityExportRequest, httpServletResponse);
    }

    @SneakyThrows
    private <T> T getResponseOrEmpty(Supplier<T> responseSupplier, Supplier<T> emptyResponseSupplier) {
        try {
            return responseSupplier.get();
        } catch (Exception e) {
            if (e instanceof ServiceException serviceException
                    && (Objects.equals(serviceException.getErrorCode(), ErrorCode.FORBIDDEN_PERMISSION.getErrorCode()) ||
                    Objects.equals(serviceException.getErrorCode(), ErrorCode.NO_DATA_PERMISSION.getErrorCode()))) {
                return emptyResponseSupplier.get();
            } else {
                throw e;
            }
        }
    }
}
