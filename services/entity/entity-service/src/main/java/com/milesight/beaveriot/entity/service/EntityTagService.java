package com.milesight.beaveriot.entity.service;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.model.EntityTag;
import com.milesight.beaveriot.entity.enums.EntityErrorCode;
import com.milesight.beaveriot.entity.enums.EntityTagMappingOperation;
import com.milesight.beaveriot.entity.model.request.EntityTagQuery;
import com.milesight.beaveriot.entity.model.request.EntityTagUpdateRequest;
import com.milesight.beaveriot.entity.model.response.EntityTagResponse;
import com.milesight.beaveriot.entity.po.EntityTagMappingPO;
import com.milesight.beaveriot.entity.po.EntityTagPO;
import com.milesight.beaveriot.entity.po.EntityTagProjection;
import com.milesight.beaveriot.entity.repository.EntityTagMappingRepository;
import com.milesight.beaveriot.entity.repository.EntityTagRepository;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
public class EntityTagService {

    public static final Integer MAX_NUMBER_OF_TAGS = 300;

    public static final Integer MAX_NUMBER_OF_TAGS_PER_ENTITY = 10;

    public static final String LOCK_NAME = "entity_tag_modification";

    @Lazy
    @Autowired
    private EntityService entityService;

    @Autowired
    private EntityTagRepository entityTagRepository;

    @Autowired
    private EntityTagMappingRepository entityTagMappingRepository;


    public Page<EntityTagResponse> search(EntityTagQuery query) {
        return (StringUtils.hasText(query.getKeyword())
                ? entityTagRepository.search(query.getKeyword(), query.toPageable())
                : entityTagRepository.search(query.toPageable()))
                .map(this::convertToResponse);
    }

    public Long count() {
        return entityTagRepository.count();
    }

    @Transactional
    @DistributedLock(name = LOCK_NAME, lockAtLeastFor = "0s", lockAtMostFor = "10s", scope = LockScope.TENANT, throwOnLockFailure = false)
    public EntityTagResponse create(EntityTagUpdateRequest request) {
        val total = count();
        if (total >= MAX_NUMBER_OF_TAGS) {
            throw new ServiceException(EntityErrorCode.NUMBER_OF_ENTITY_TAGS_EXCEEDED);
        }

        if (entityTagRepository.existsByName(request.getName())) {
            throw new ServiceException(EntityErrorCode.ENTITY_TAG_NAME_ALREADY_EXISTS);
        }

        val entityTag = EntityTagPO.builder()
                .id(SnowflakeUtil.nextId())
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor())
                .build();
        return convertToResponse(entityTagRepository.save(entityTag));
    }

    @Transactional
    public void update(Long id, EntityTagUpdateRequest request) {
        EntityTagPO entityTagPO = entityTagRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND));

        if (!Objects.equals(request.getName(), entityTagPO.getName())
                && entityTagRepository.existsByName(request.getName())) {
            throw new ServiceException(EntityErrorCode.ENTITY_TAG_NAME_ALREADY_EXISTS);
        }

        entityTagPO.setName(request.getName());
        entityTagPO.setDescription(request.getDescription());
        entityTagPO.setColor(request.getColor());
        entityTagRepository.save(entityTagPO);
    }

    private EntityTagResponse convertToResponse(EntityTagProjection projection) {
        return EntityTagResponse.builder()
                .id(String.valueOf(projection.getId()))
                .name(projection.getName())
                .description(projection.getDescription())
                .color(projection.getColor())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .taggedEntitiesCount(projection.getTaggedEntitiesCount())
                .build();
    }

    private EntityTagResponse convertToResponse(EntityTagPO po) {
        return EntityTagResponse.builder()
                .id(String.valueOf(po.getId()))
                .name(po.getName())
                .description(po.getDescription())
                .color(po.getColor())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .taggedEntitiesCount(po.getTaggedEntitiesCount())
                .build();
    }

    @Transactional
    @DistributedLock(name = LOCK_NAME, lockAtLeastFor = "0s", lockAtMostFor = "10s", scope = LockScope.TENANT, throwOnLockFailure = false)
    public void delete(List<Long> tagIds) {
        entityTagRepository.deleteAllById(tagIds);
        entityTagMappingRepository.deleteByTagIdIn(tagIds);
    }

    @Transactional
    @DistributedLock(name = LOCK_NAME, lockAtLeastFor = "0s", lockAtMostFor = "10s", scope = LockScope.TENANT, throwOnLockFailure = false)
    public void handleEntityTagMappings(EntityTagMappingOperation operation, List<Long> entityIds, List<Long> removedTagIds, List<Long> addedTagIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }

        switch (operation) {
            case ADD -> addTagsToEntities(entityIds, addedTagIds);
            case OVERWRITE -> overwriteTagsOnEntities(entityIds, addedTagIds);
            case REMOVE -> removeTagsFromEntities(entityIds, removedTagIds);
            case REPLACE -> replaceTagsOnEntities(entityIds, removedTagIds, addedTagIds);
        }
    }

    @Transactional
    public void addTagsToEntities(List<Long> entityIds, List<Long> tagIds) {
        if (entityIds == null || entityIds.isEmpty() || tagIds == null || tagIds.isEmpty()) {
            return;
        }

        val entities = entityService.listEntityPOById(entityIds);
        if (entities.isEmpty()) {
            return;
        }

        val tags = entityTagRepository.findAllById(tagIds);
        if (tags.size() < tagIds.size()) {
            throw new ServiceException(EntityErrorCode.ENTITY_TAG_NOT_FOUND);
        }

        val entityIdToTagIds = entityTagMappingRepository.findByEntityIdIn(entityIds)
                .stream()
                .collect(Collectors.groupingBy(EntityTagMappingPO::getEntityId, Collectors.mapping(EntityTagMappingPO::getTagId, Collectors.toSet())));
        if (entityIdToTagIds.entrySet().stream()
                .anyMatch(entry -> Stream.concat(entry.getValue().stream(), tagIds.stream()).distinct().count() > MAX_NUMBER_OF_TAGS_PER_ENTITY)) {
            throw new ServiceException(EntityErrorCode.NUMBER_OF_TAGS_PER_ENTITY_EXCEEDED);
        }

        val newEntityTagMappings = entities.stream()
                .flatMap(entity -> tagIds.stream()
                        .map(tagId -> EntityTagMappingPO.builder()
                                .id(SnowflakeUtil.nextId())
                                .entityId(entity.getId())
                                .tagId(tagId)
                                .build()))
                .filter(mapping -> {
                    val existingTags = entityIdToTagIds.get(mapping.getEntityId());
                    return CollectionUtils.isEmpty(existingTags) || !existingTags.contains(mapping.getTagId());
                })
                .collect(Collectors.toList());
        entityTagMappingRepository.saveAll(newEntityTagMappings);
    }

    @Transactional
    public void overwriteTagsOnEntities(List<Long> entityIds, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        entityTagMappingRepository.deleteByEntityIdIn(entityIds);
        addTagsToEntities(entityIds, tagIds);
    }

    @Transactional
    public void removeTagsFromEntities(List<Long> entityIds, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        entityTagMappingRepository.deleteByTagIdInAndEntityIdIn(tagIds, entityIds);
    }

    @Transactional
    public void replaceTagsOnEntities(List<Long> entityIds, List<Long> removedTagIds, List<Long> addedTagIds) {
        if (removedTagIds == null || removedTagIds.isEmpty() || addedTagIds == null || addedTagIds.isEmpty()) {
            return;
        }

        val matchedMappings = entityTagMappingRepository.findByTagIdInAndEntityIdIn(removedTagIds, entityIds);
        val targetEntityIds = matchedMappings.stream()
                .map(EntityTagMappingPO::getEntityId)
                .distinct()
                .collect(Collectors.toList());

        if (targetEntityIds.isEmpty()) {
            return;
        }

        entityTagMappingRepository.deleteByTagIdInAndEntityIdIn(removedTagIds, targetEntityIds);
        addTagsToEntities(targetEntityIds, addedTagIds);
    }

    public Map<Long, List<EntityTag>> entityIdToTags(List<Long> entityIds) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return Collections.emptyMap();
        }

        val mappings = entityTagMappingRepository.findByEntityIdIn(entityIds);
        val tagIds = mappings.stream()
                .map(EntityTagMappingPO::getTagId)
                .distinct()
                .collect(Collectors.toList());
        val tags = entityTagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(EntityTagPO::getId, v -> v, (v1, v2) -> v1));

        return mappings.stream()
                .collect(Collectors.groupingBy(EntityTagMappingPO::getEntityId,
                        Collectors.mapping(mapping -> {
                            val tag = tags.get(mapping.getTagId());
                            if (tag == null) {
                                return EntityTag.builder()
                                        .id(String.valueOf(mapping.getTagId()))
                                        .build();
                            }
                            return EntityTag.builder()
                                    .id(String.valueOf(tag.getId()))
                                    .name(tag.getName())
                                    .description(tag.getDescription())
                                    .color(tag.getColor())
                                    .build();
                        }, Collectors.toList())));
    }

    public List<Long> findEntityIdsByTagContains(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return Collections.emptyList();
        }
        return entityTagMappingRepository.findEntityIdsByTagContains(tagNames, tagNames.size());
    }

    public List<Long> findEntityIdsByTagNotContains(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return Collections.emptyList();
        }
        return entityTagMappingRepository.findEntityIdsByTagNotContains(tagNames);
    }

    public List<Long> findEntityIdsByTagEquals(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return Collections.emptyList();
        }
        return entityTagMappingRepository.findEntityIdsByTagEquals(tagNames);
    }

    public List<Long> findEntityIdsByTagIn(List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return Collections.emptyList();
        }
        return entityTagMappingRepository.findEntityIdsByTagIn(tagNames);
    }


    public List<Long> findEntityIdsByTagIsEmpty() {
        return entityTagMappingRepository.findEntityIdsByTagIsEmpty();
    }

    public List<Long> findEntityIdsByTagIsNotEmpty() {
        return entityTagMappingRepository.findEntityIdsByTagIsNotEmpty();
    }

    @Transactional
    public void deleteMappingsByEntityIds(List<Long> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }
        entityTagMappingRepository.deleteByEntityIdIn(entityIds);
    }

}
