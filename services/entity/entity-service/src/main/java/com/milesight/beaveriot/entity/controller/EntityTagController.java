package com.milesight.beaveriot.entity.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.entity.model.request.EntityTagDeleteRequest;
import com.milesight.beaveriot.entity.model.request.EntityTagMappingRequest;
import com.milesight.beaveriot.entity.model.request.EntityTagQuery;
import com.milesight.beaveriot.entity.model.request.EntityTagUpdateRequest;
import com.milesight.beaveriot.entity.model.response.EntityTagNumberResponse;
import com.milesight.beaveriot.entity.model.response.EntityTagResponse;
import com.milesight.beaveriot.entity.service.EntityTagService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/entity/tags")
public class EntityTagController {

    @Autowired
    private EntityTagService entityTagService;

    @PostMapping("/search")
    public ResponseBody<Page<EntityTagResponse>> search(@RequestBody EntityTagQuery entityTagQuery) {
        return ResponseBuilder.success(entityTagService.search(entityTagQuery));
    }

    @GetMapping("/number")
    public ResponseBody<EntityTagNumberResponse> count() {
        val number = entityTagService.count();
        return ResponseBuilder.success(new EntityTagNumberResponse(number));
    }

    @OperationPermission(codes = {OperationPermissionCode.ENTITY_TAG_MANAGE})
    @PostMapping
    public ResponseBody<EntityTagResponse> create(@RequestBody @Valid EntityTagUpdateRequest request) {
        return ResponseBuilder.success(entityTagService.create(request));
    }

    @OperationPermission(codes = {OperationPermissionCode.ENTITY_TAG_MANAGE})
    @PutMapping("/{tagId}")
    public ResponseBody<Void> update(@PathVariable("tagId") Long tagId, @RequestBody @Valid EntityTagUpdateRequest request) {
        entityTagService.update(tagId, request);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.ENTITY_TAG_MANAGE})
    @PostMapping("/delete")
    public ResponseBody<Void> delete(@RequestBody @Valid EntityTagDeleteRequest request) {
        entityTagService.delete(request.getIds());
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.ENTITY_DATA_EDIT})
    @PostMapping("/mapping")
    public ResponseBody<Void> mapping(@RequestBody @Valid EntityTagMappingRequest request) {
        entityTagService.handleEntityTagMappings(request.getOperation(), request.getEntityIds(), request.getRemovedTagIds(), request.getAddedTagIds());
        return ResponseBuilder.success();
    }

}
