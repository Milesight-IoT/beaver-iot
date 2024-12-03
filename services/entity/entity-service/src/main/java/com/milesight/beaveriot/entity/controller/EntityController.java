package com.milesight.beaveriot.entity.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.entity.model.request.EntityAggregateQuery;
import com.milesight.beaveriot.entity.model.request.EntityHistoryQuery;
import com.milesight.beaveriot.entity.model.request.EntityQuery;
import com.milesight.beaveriot.entity.model.request.ServiceCallRequest;
import com.milesight.beaveriot.entity.model.request.UpdatePropertyEntityRequest;
import com.milesight.beaveriot.entity.model.response.EntityAggregateResponse;
import com.milesight.beaveriot.entity.model.response.EntityHistoryResponse;
import com.milesight.beaveriot.entity.model.response.EntityLatestResponse;
import com.milesight.beaveriot.entity.model.response.EntityMetaResponse;
import com.milesight.beaveriot.entity.model.response.EntityResponse;
import com.milesight.beaveriot.entity.service.EntityService;
import com.milesight.beaveriot.entity.service.EntityValueService;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/search")
    public ResponseBody<Page<EntityResponse>> search(@RequestBody EntityQuery entityQuery) {
        Page<EntityResponse> entityResponse = entityService.search(entityQuery);
        return ResponseBuilder.success(entityResponse);
    }

    @GetMapping("/{entityId}/children")
    public ResponseBody<List<EntityResponse>> getChildren(@PathVariable("entityId") Long entityId) {
        List<EntityResponse> entityResponse = entityService.getChildren(entityId);
        return ResponseBuilder.success(entityResponse);
    }

    @PostMapping("/history/search")
    public ResponseBody<Page<EntityHistoryResponse>> historySearch(@RequestBody EntityHistoryQuery entityHistoryQuery) {
        Page<EntityHistoryResponse> entityHistoryResponse = entityValueService.historySearch(entityHistoryQuery);
        return ResponseBuilder.success(entityHistoryResponse);
    }

    @PostMapping("/history/aggregate")
    public ResponseBody<EntityAggregateResponse> historyAggregate(@RequestBody EntityAggregateQuery entityAggregateQuery) {
        EntityAggregateResponse entityAggregateResponse = entityValueService.historyAggregate(entityAggregateQuery);
        return ResponseBuilder.success(entityAggregateResponse);
    }

    @GetMapping("/{entityId}/status")
    public ResponseBody<EntityLatestResponse> getEntityStatus(@PathVariable("entityId") Long entityId) {
        EntityLatestResponse entityLatestResponse = entityValueService.getEntityStatus(entityId);
        return ResponseBuilder.success(entityLatestResponse);
    }

    @GetMapping("/{entityId}/meta")
    public ResponseBody<EntityMetaResponse> getEntityMeta(@PathVariable("entityId") Long entityId) {
        EntityMetaResponse entityMetaResponse = entityService.getEntityMeta(entityId);
        return ResponseBuilder.success(entityMetaResponse);
    }

    @PostMapping("/property/update")
    public ResponseBody<Void> updatePropertyEntity(@RequestBody UpdatePropertyEntityRequest updatePropertyEntityRequest) {
        entityService.updatePropertyEntity(updatePropertyEntityRequest);
        return ResponseBuilder.success();
    }

    @PostMapping("/service/call")
    public ResponseBody<EventResponse> serviceCall(@RequestBody ServiceCallRequest serviceCallRequest) {
        EventResponse eventResponse = entityService.serviceCall(serviceCallRequest);
        return ResponseBuilder.success(eventResponse);
    }

}
