package com.milesight.beaveriot.entity.service;

import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict;
import com.milesight.beaveriot.base.enums.ComparisonOperator;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.base.exception.MultipleErrorException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.enums.ResourceRefType;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.model.event.EntityEvent;
import com.milesight.beaveriot.context.model.EntityTag;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.util.PageConverter;
import com.milesight.beaveriot.device.dto.DeviceGroupDTO;
import com.milesight.beaveriot.device.dto.DeviceIdKeyDTO;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.device.facade.IDeviceGroupFacade;
import com.milesight.beaveriot.entity.dto.EntityDeviceGroup;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.dto.EntityWorkflowData;
import com.milesight.beaveriot.entity.enums.EntitySearchColumn;
import com.milesight.beaveriot.entity.model.dto.EntityAdvancedSearchCondition;
import com.milesight.beaveriot.entity.model.request.EntityAdvancedSearchQuery;
import com.milesight.beaveriot.entity.model.request.EntityCreateRequest;
import com.milesight.beaveriot.entity.model.request.EntityModifyRequest;
import com.milesight.beaveriot.entity.model.request.ServiceCallRequest;
import com.milesight.beaveriot.entity.model.request.UpdatePropertyEntityRequest;
import com.milesight.beaveriot.entity.model.response.EntityMetaResponse;
import com.milesight.beaveriot.entity.po.EntityLatestPO;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityLatestRepository;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.permission.facade.IPermissionFacade;
import com.milesight.beaveriot.context.model.ResourceRefDTO;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import com.milesight.beaveriot.rule.dto.WorkflowNameDTO;
import com.milesight.beaveriot.rule.facade.IWorkflowFacade;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author loong
 * @date 2024/10/16 14:22
 */
@Service
@Slf4j
public class EntityService implements EntityServiceProvider {

    @Autowired
    private IDeviceFacade deviceFacade;

    @Lazy
    @Autowired
    private IntegrationServiceProvider integrationServiceProvider;

    @Autowired
    private IUserFacade userFacade;

    @Autowired
    private EventBus<EntityEvent> eventBus;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityLatestRepository entityLatestRepository;
    @Autowired
    private EntityValueService entityValueService;

    @Autowired
    private EntityTagService entityTagService;

    @Autowired
    private IWorkflowFacade workflowFacade;

    @Autowired
    IPermissionFacade permissionFacade;

    @Autowired
    ResourceManagerFacade resourceManagerFacade;

    @Autowired
    IDeviceGroupFacade deviceGroupFacade;

    private static Entity convertPOToEntity(EntityPO entityPO, Map<String, DeviceNameDTO> deviceIdToDetails) {
        String integrationId = null;
        String deviceKey = null;
        String attachTargetId = entityPO.getAttachTargetId();
        AttachTargetType attachTarget = entityPO.getAttachTarget();
        if (attachTarget == AttachTargetType.DEVICE) {
            DeviceNameDTO deviceDetail = deviceIdToDetails.get(attachTargetId);
            if (deviceDetail != null) {
                deviceKey = deviceDetail.getKey();
                integrationId = deviceDetail.getIntegrationId();
            }
        } else if (attachTarget == AttachTargetType.INTEGRATION) {
            integrationId = attachTargetId;
        }
        return convertPOToEntity(entityPO, integrationId, deviceKey);
    }

    private static Entity convertPOToEntity(EntityPO entityPO, String integrationId, String deviceKey) {
        EntityBuilder entityBuilder = new EntityBuilder(integrationId, deviceKey)
                .id(entityPO.getId())
                .identifier(entityPO.getKey().substring(entityPO.getKey().lastIndexOf(".") + 1))
                .valueType(entityPO.getValueType())
                .visible(entityPO.getVisible())
                .description(entityPO.getDescription())
                .attributes(entityPO.getValueAttribute())
                .valueStoreMod(entityPO.getValueStoreMod());

        String parentKey = entityPO.getParent();
        if (StringUtils.hasText(entityPO.getParent())) {
            String parentIdentifier = entityPO.getParent().substring(parentKey.lastIndexOf(".") + 1);
            entityBuilder.parentIdentifier(parentIdentifier);
        }

        Entity entity = null;
        if (entityPO.getType() == EntityType.PROPERTY) {
            entity = entityBuilder.property(entityPO.getName(), entityPO.getAccessMod())
                    .build();
        } else if (entityPO.getType() == EntityType.SERVICE) {
            entity = entityBuilder.service(entityPO.getName())
                    .build();
        } else if (entityPO.getType() == EntityType.EVENT) {
            entity = entityBuilder.event(entityPO.getName())
                    .build();
        }
        return entity;
    }

    private static EntityMetaResponse convertEntityPOToEntityMetaResponse(EntityPO entityPO) {
        EntityMetaResponse response = new EntityMetaResponse();
        response.setId(entityPO.getId());
        response.setKey(entityPO.getKey());
        response.setName(entityPO.getName());
        response.setType(entityPO.getType());
        response.setAccessMod(entityPO.getAccessMod());
        response.setValueStoreMod(entityPO.getValueStoreMod());
        response.setValueAttribute(entityPO.getValueAttribute());
        response.setValueType(entityPO.getValueType());
        response.setCustomized(entityPO.checkIsCustomizedEntity());
        response.setCreatedAt(entityPO.getCreatedAt());
        response.setUpdatedAt(entityPO.getUpdatedAt());
        response.setDescription(entityPO.getDescription());
        return response;
    }

    private EntityPO saveConvert(Long userId, Entity entity, Map<String, Long> deviceKeyMap, Map<String, EntityPO> dataEntityKeyMap) {
        try {
            AttachTargetType attachTarget;
            String attachTargetId;
            if (StringUtils.hasText(entity.getDeviceKey())) {
                attachTarget = AttachTargetType.DEVICE;
                attachTargetId = deviceKeyMap.get(entity.getDeviceKey()) == null ? null : String.valueOf(deviceKeyMap.get(entity.getDeviceKey()));
                if (attachTargetId == null) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                }
            } else {
                attachTarget = AttachTargetType.INTEGRATION;
                attachTargetId = entity.getIntegrationId();
            }
            EntityPO entityPO = new EntityPO();
            Long entityId;
            EntityPO dataEntityPO = dataEntityKeyMap.get(entity.getKey());
            if (dataEntityPO == null) {
                entityId = SnowflakeUtil.nextId();
            } else {
                entityId = dataEntityPO.getId();
                entityPO.setCreatedAt(dataEntityPO.getCreatedAt());
            }
            entityPO.setId(entityId);
            entityPO.setUserId(userId);
            entityPO.setKey(entity.getKey());
            entityPO.setName(entity.getName());
            entityPO.setType(entity.getType());
            entityPO.setAccessMod(entity.getAccessMod());
            entityPO.setValueStoreMod(entity.getValueStoreMod());
            entityPO.setParent(entity.getParentKey());
            entityPO.setAttachTarget(attachTarget);
            entityPO.setAttachTargetId(attachTargetId);
            entityPO.setValueAttribute(entity.getAttributes());
            entityPO.setValueType(entity.getValueType());
            entityPO.setVisible(entity.getVisible());
            entityPO.setDescription(entity.getDescription());
            return entityPO;
        } catch (Exception e) {
            log.error("save entity error:{}", e.getMessage(), e);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

    @Override
    @NonNull
    public List<Entity> findByTargetId(AttachTargetType targetType, String targetId) {
        if (!StringUtils.hasText(targetId)) {
            return new ArrayList<>();
        }
        return findByTargetIds(targetType, Collections.singletonList(targetId));
    }

    public List<Entity> convertPOListToEntities(List<EntityPO> entityPOList) {
        if (entityPOList == null || entityPOList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> deviceIds = entityPOList.stream()
                .filter(entityPO -> AttachTargetType.DEVICE.equals(entityPO.getAttachTarget()))
                .map(EntityPO::getAttachTargetId)
                .distinct()
                .map(Long::valueOf)
                .toList();
        Map<String, DeviceNameDTO> deviceIdToDetails = deviceIdToDetails(deviceIds);

        Map<String, List<Entity>> parentKeyToChildren = entityPOList.stream()
                .filter(entityPO -> StringUtils.hasText(entityPO.getParent()))
                .map(entityPO -> convertPOToEntity(entityPO, deviceIdToDetails))
                .collect(Collectors.groupingBy(Entity::getParentKey));

        List<Entity> parentEntities = entityPOList.stream()
                .filter(entityPO -> !StringUtils.hasText(entityPO.getParent()))
                .map(entityPO -> {
                    Entity entity = convertPOToEntity(entityPO, deviceIdToDetails);
                    List<Entity> children = parentKeyToChildren.get(entity.getKey());
                    if (children != null) {
                        entity.setChildren(children);
                        parentKeyToChildren.remove(entity.getKey());
                    }
                    return entity;
                })
                .toList();

        return Stream.concat(parentEntities.stream(),
                        // include all children that have no parent
                        parentKeyToChildren.values().stream().flatMap(List::stream))
                .toList();
    }

    private Map<String, DeviceNameDTO> deviceIdToDetails(List<Long> deviceIds) {
        Map<String, DeviceNameDTO> deviceIdToDetails = new HashMap<>();
        if (deviceIds.isEmpty()) {
            return deviceIdToDetails;
        }
        List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIds(deviceIds);
        if (deviceNameDTOList != null && !deviceNameDTOList.isEmpty()) {
            deviceIdToDetails.putAll(deviceNameDTOList.stream()
                    .collect(Collectors.toMap(v -> String.valueOf(v.getId()), Function.identity(), (v1, v2) -> v1)));
        }
        return deviceIdToDetails;
    }

    @Override
    @NonNull
    public List<Entity> findByTargetIds(AttachTargetType targetType, List<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.attachTargetId, targetIds.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return convertPOListToEntities(entityPOList);
        } catch (Exception e) {
            log.error("find entity by targetId error:{}", e.getMessage(), e);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

    @Override
    public void save(Entity entity) {
        Long userId = SecurityUserContext.getUserId();
        if (entity == null) {
            return;
        }
        List<Entity> allEntityList = new ArrayList<>();
        allEntityList.add(entity);
        if (!CollectionUtils.isEmpty(entity.getChildren())) {
            allEntityList.addAll(entity.getChildren());
        }
        doBatchSaveEntity(userId, allEntityList);
    }

    @Override
    public void batchSave(List<Entity> entityList) {
        Long userId = SecurityUserContext.getUserId();
        if (entityList == null || entityList.isEmpty()) {
            return;
        }
        List<Entity> allEntityList = new ArrayList<>(entityList);
        entityList.forEach(entity -> {
            List<Entity> childrenEntityList = entity.getChildren();
            if (childrenEntityList != null && !childrenEntityList.isEmpty()) {
                allEntityList.addAll(childrenEntityList);
            }
        });
        doBatchSaveEntity(userId, allEntityList);
    }

    private void doBatchSaveEntity(Long userId, List<Entity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return;
        }
        List<String> deviceKeys = entityList.stream().map(Entity::getDeviceKey).filter(StringUtils::hasText).toList();
        Map<String, Long> deviceKeyMap = new HashMap<>();
        if (!deviceKeys.isEmpty()) {
            List<DeviceIdKeyDTO> deviceIdKeyDTOList = deviceFacade.findIdAndKeyByKeys(deviceKeys);
            if (!deviceIdKeyDTOList.isEmpty()) {
                deviceKeyMap.putAll(deviceIdKeyDTOList.stream().collect(Collectors.toMap(DeviceIdKeyDTO::getKey, DeviceIdKeyDTO::getId)));
            }
        }
        List<String> entityKeys = entityList.stream().map(Entity::getKey).filter(StringUtils::hasText).toList();
        List<EntityPO> dataEntityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        Map<String, EntityPO> dataEntityKeyMap = new HashMap<>();
        List<EntityPO> deleteEntityPOList = new ArrayList<>();
        if (dataEntityPOList != null && !dataEntityPOList.isEmpty()) {
            dataEntityKeyMap.putAll(dataEntityPOList.stream().collect(Collectors.toMap(EntityPO::getKey, Function.identity())));

            List<String> parentEntityKeys = dataEntityPOList.stream()
                    .filter(t -> t.getParent() == null)
                    .map(EntityPO::getKey)
                    .distinct()
                    .toList();
            if (!parentEntityKeys.isEmpty()) {
                List<EntityPO> childrenEntityPOList = entityRepository.findAll(
                        filter -> filter.in(EntityPO.Fields.parent, parentEntityKeys.toArray()));
                childrenEntityPOList.forEach(entityPO -> {
                    if (!entityKeys.contains(entityPO.getKey())) {
                        deleteEntityPOList.add(entityPO);
                    }
                });
            }
        }

        List<EntityPO> entityPOList = new ArrayList<>();

        entityList.forEach(t -> {
            EntityPO entityPO = saveConvert(userId, t, deviceKeyMap, dataEntityKeyMap);
            EntityPO dataEntityPO = dataEntityKeyMap.get(t.getKey());
            if (dataEntityPO == null
                    || dataEntityPO.getAccessMod() != entityPO.getAccessMod()
                    || !Objects.equals(dataEntityPO.getValueStoreMod(), entityPO.getValueStoreMod())
                    || dataEntityPO.getValueType() != entityPO.getValueType()
                    || !Objects.equals(JsonUtils.toJSON(dataEntityPO.getValueAttribute()), JsonUtils.toJSON(entityPO.getValueAttribute()))
                    || dataEntityPO.getType() != entityPO.getType()
                    || !dataEntityPO.getName().equals(entityPO.getName())
                    || !dataEntityPO.getVisible().equals(entityPO.getVisible())
                    || !Objects.equals(dataEntityPO.getDescription(), entityPO.getDescription())
                    || !Objects.equals(dataEntityPO.getAttachTarget(), entityPO.getAttachTarget())
                    || !Objects.equals(dataEntityPO.getAttachTargetId(), entityPO.getAttachTargetId())) {
                entityPOList.add(entityPO);
            }
        });

        batchValidateEntityPOs(entityPOList);

        if (!deleteEntityPOList.isEmpty()) {
            entityRepository.deleteAll(deleteEntityPOList);
        }

        entityRepository.saveAll(entityPOList);

        entityList.forEach(entity -> {
            boolean isCreate = dataEntityKeyMap.get(entity.getKey()) == null;
            if (isCreate) {
                eventBus.publish(EntityEvent.of(EntityEvent.EventType.CREATED, entity));
            } else {
                if (entityPOList.stream().anyMatch(entityPO -> entityPO.getKey().equals(entity.getKey()))) {
                    eventBus.publish(EntityEvent.of(EntityEvent.EventType.UPDATED, entity));
                }
            }
        });
    }

    private void batchValidateEntityPOs(List<EntityPO> entityPOList) {
        List<ErrorHolder> errors = new ArrayList<>();
        entityPOList.forEach(entityPO -> {
            List<ErrorHolder> entityPOErrors = entityPO.validate();
            if (!CollectionUtils.isEmpty(entityPOErrors)) {
                errors.addAll(entityPOErrors);
            }
        });
        if (!CollectionUtils.isEmpty(errors)) {
            throw MultipleErrorException.with(HttpStatus.BAD_REQUEST.value(), "Validate entity error", errors);
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteByTargetId(String targetId) {
        if (!StringUtils.hasText(targetId)) {
            return;
        }
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.eq(EntityPO.Fields.attachTargetId, targetId));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return;
        }

        self().deleteEntitiesByPOList(entityPOList);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteByKey(String entityKey) {
        if (!StringUtils.hasText(entityKey)) {
            return;
        }
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.or(filter1 -> filter1.eq(EntityPO.Fields.key, entityKey).eq(EntityPO.Fields.parent, entityKey)));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return;
        }

        self().deleteEntitiesByPOList(entityPOList);
    }

    @BatchCacheEvict(cacheNames = CacheKeyConstants.ENTITY_LATEST_VALUE_CACHE_NAME, key = "#result", keyPrefix = CacheKeyConstants.TENANT_PREFIX)
    public List<String> deleteEntitiesByPOList(List<EntityPO> entityPOList) {
        if (entityPOList.isEmpty()) {
            return List.of();
        }

        List<Long> entityIdList = entityPOList.stream().map(EntityPO::getId).toList();
        log.info("delete entities: {}", entityIdList);

        entityRepository.deleteAllById(entityIdList);
        entityValueService.deleteEntityHistory(entityIdList);
        List<EntityLatestPO> entityLatestPOS = entityLatestRepository.findByEntityIdIn(entityIdList);
        if (!CollectionUtils.isEmpty(entityLatestPOS)) {
            List<Long> entityLatestIds = entityLatestPOS.stream().map(EntityLatestPO::getId).toList();
            entityLatestRepository.deleteAll(entityLatestPOS);
            entityLatestIds.forEach(entityLatestId -> resourceManagerFacade.unlinkRefAsync(ResourceRefDTO.of(String.valueOf(entityLatestId), ResourceRefType.ENTITY_LATEST.name())));
        }
        entityTagService.deleteMappingsByEntityIds(entityIdList);
        userFacade.deleteResource(ResourceType.ENTITY, entityIdList);

        List<Entity> entityList = convertPOListToEntities(entityPOList);
        entityList.forEach(entity ->
                eventBus.publish(EntityEvent.of(EntityEvent.EventType.DELETED, entity)));
        return entityPOList.stream().map(EntityPO::getKey).toList();
    }

    @Override
    public Entity findByKey(String entityKey) {
        Map<String, Entity> entityMap = findByKeys(List.of(entityKey));
        return entityMap.get(entityKey);
    }

    @Override
    public Map<String, Entity> findByKeys(Collection<String> entityKeys) {
        if (entityKeys == null || entityKeys.isEmpty()) {
            return new HashMap<>();
        }
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return new HashMap<>();
        }
        List<EntityPO> childrenEntityPOList = findChildrenEntityPOListByParents(entityPOList);

        List<EntityPO> parentAndChildren = Stream.concat(entityPOList.stream(), childrenEntityPOList.stream()).toList();
        try {
            List<Entity> entities = convertPOListToEntities(parentAndChildren);
            return mapKeysToEntities(entities, entityKeys, Entity::getKey)
                    .stream()
                    .collect(Collectors.toMap(Entity::getKey, Function.identity()));
        } catch (Exception e) {
            log.error("find entity by keys error: {}", e.getMessage(), e);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

    @Override
    public Entity findById(Long id) {
        return findByIds(Collections.singletonList(id))
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Entity> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<EntityPO> entityPOList = findEntityPOListAndTheirChildrenByIds(ids);
        if (entityPOList == null || entityPOList.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<Entity> entities = convertPOListToEntities(entityPOList);
            return mapKeysToEntities(entities, ids, Entity::getId);
        } catch (Exception e) {
            log.error("find entity by ids error: {}", e.getMessage(), e);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

    @Override
    public Map<Long, List<EntityTag>> findTagsByIds(List<Long> entityIds) {
        return entityTagService.entityIdToTags(entityIds);
    }

    public Page<EntityResponse> search(EntityQuery entityQuery) {
        boolean isExcludeChildren = entityQuery.getExcludeChildren() != null && entityQuery.getExcludeChildren();
        List<String> attachTargetIds = searchAttachTargetIdsByKeyword(entityQuery.getKeyword());

        boolean hasEntityCustomViewPermission = permissionFacade.hasMenuPermission(OperationPermissionCode.ENTITY_CUSTOM_VIEW);
        if (!hasEntityCustomViewPermission) {
            entityQuery.setCustomized(false);
        }
        List<String> sourceTargetIds = searchAttachTargetIdsByKeyword(entityQuery.getEntitySourceName());
        if (StringUtils.hasText(entityQuery.getEntitySourceName()) && sourceTargetIds.isEmpty()) {
            return Page.empty();
        }
        Consumer<Filterable> filterable = f -> f.isNull(isExcludeChildren, EntityPO.Fields.parent)
                .in(!CollectionUtils.isEmpty(entityQuery.getEntityType()), EntityPO.Fields.type, toArray(entityQuery.getEntityType()))
                .in(!CollectionUtils.isEmpty(entityQuery.getEntityIds()), EntityPO.Fields.id, toArray(entityQuery.getEntityIds()))
                .in(!CollectionUtils.isEmpty(entityQuery.getEntityValueType()), EntityPO.Fields.valueType, toArray(entityQuery.getEntityValueType()))
                .in(!CollectionUtils.isEmpty(entityQuery.getEntityAccessMod()), EntityPO.Fields.accessMod, toArray(entityQuery.getEntityAccessMod()))
                .in(!CollectionUtils.isEmpty(entityQuery.getEntityKeys()), EntityPO.Fields.key, toArray(entityQuery.getEntityKeys()))
                .in(!CollectionUtils.isEmpty(entityQuery.getEntityNames()), EntityPO.Fields.name, toArray(entityQuery.getEntityNames()))
                .eq(Boolean.TRUE.equals(entityQuery.getCustomized()), EntityPO.Fields.attachTargetId, IntegrationConstants.SYSTEM_INTEGRATION_ID)
                .ne(Boolean.FALSE.equals(entityQuery.getCustomized()), EntityPO.Fields.attachTargetId, IntegrationConstants.SYSTEM_INTEGRATION_ID)
                .eq(!Boolean.TRUE.equals(entityQuery.getShowHidden()), EntityPO.Fields.visible, true)
                .in(!CollectionUtils.isEmpty(sourceTargetIds), EntityPO.Fields.attachTargetId, toArray(sourceTargetIds))
                .or(f1 -> f1.likeIgnoreCase(StringUtils.hasText(entityQuery.getKeyword()), EntityPO.Fields.name, entityQuery.getKeyword())
                        .likeIgnoreCase(StringUtils.hasText(entityQuery.getKeyword()) && !Boolean.TRUE.equals(entityQuery.getNotScanKey()), EntityPO.Fields.key, entityQuery.getKeyword())
                        .in(!CollectionUtils.isEmpty(attachTargetIds), EntityPO.Fields.attachTargetId, toArray(attachTargetIds)));
        List<EntityPO> entityPOList = new ArrayList<>();
        if (!Boolean.TRUE.equals(entityQuery.getCustomized())) {
            try {
                entityPOList = entityRepository.findAllWithDataPermission(filterable);
            } catch (Exception e) {
                if (e instanceof ServiceException serviceException
                        && (Objects.equals(serviceException.getErrorCode(), ErrorCode.FORBIDDEN_PERMISSION.getErrorCode()) ||
                        Objects.equals(serviceException.getErrorCode(), ErrorCode.NO_DATA_PERMISSION.getErrorCode()))) {
                    entityPOList = new ArrayList<>();
                } else {
                    throw e;
                }
            }
        }
        // custom entities belong to system integration, don`t use permission , so we need to add them to the list
        if (!Boolean.FALSE.equals(entityQuery.getCustomized())) {
            List<EntityPO> entityCustomizedPOList = entityRepository.findAll(filterable.andThen(f1 -> f1.eq(EntityPO.Fields.attachTargetId, IntegrationConstants.SYSTEM_INTEGRATION_ID)));
            entityPOList.addAll(entityCustomizedPOList);
        }
        if (entityPOList == null || entityPOList.isEmpty()) {
            return Page.empty();
        }

        entityPOList = entityPOList.stream().distinct().toList();
        if (entityQuery.getSort().getOrders().isEmpty()) {
            entityQuery.sort(new Sorts().desc(EntityPO.Fields.id));
        }

        Page<EntityPO> entityPOPage = PageConverter.convertToPage(entityPOList, entityQuery.toPageable());

        return convertEntityPOPageToEntityResponses(entityPOPage);
    }

    private EntityResponse convertEntityPOToEntityResponse(EntityPO entityPO,
                                                           Map<String, Integration> integrationMap,
                                                           Map<String, DeviceNameDTO> deviceIdToDetails,
                                                           Map<String, EntityPO> parentKeyMap,
                                                           Map<Long, WorkflowNameDTO> entityWorkflowMap,
                                                           Map<Long, DeviceGroupDTO> deviceGroupDTOMap) {
        String deviceName = null;
        String deviceGroupId = null;
        String deviceGroupName = null;
        String integrationName = null;
        String attachTargetId = entityPO.getAttachTargetId();
        AttachTargetType attachTarget = entityPO.getAttachTarget();
        if (attachTarget == AttachTargetType.DEVICE) {
            DeviceNameDTO deviceDetail = deviceIdToDetails.get(attachTargetId);
            if (deviceDetail != null) {
                deviceName = deviceDetail.getName();
                DeviceGroupDTO deviceGroupDTO = deviceGroupDTOMap.get(deviceDetail.getId());
                if (deviceGroupDTO != null) {
                    deviceGroupId = String.valueOf(deviceGroupDTO.getGroupId());
                    deviceGroupName = deviceGroupDTO.getGroupName();
                }
                if (deviceDetail.isIntegrationExists()) {
                    integrationName = deviceDetail.getIntegrationName();
                }
            }
        } else if (attachTarget == AttachTargetType.INTEGRATION) {
            Integration integration = integrationMap.get(attachTargetId);
            if (integration != null) {
                integrationName = integration.getName();
            }
        }

        final EntityPO parentEntity = parentKeyMap.get(entityPO.getParent());
        final EntityDeviceGroup deviceGroup = deviceGroupId == null ? null : new EntityDeviceGroup(deviceGroupId, deviceGroupName);

        EntityResponse response = new EntityResponse();
        response.setDeviceName(deviceName);
        response.setIntegrationName(integrationName);
        response.setEntityId(entityPO.getId().toString());
        response.setEntityAccessMod(entityPO.getAccessMod());
        response.setEntityValueStoreMod(entityPO.getValueStoreMod());
        response.setEntityKey(entityPO.getKey());
        response.setEntityType(entityPO.getType());
        response.setEntityName(entityPO.getName());
        response.setEntityParentName(parentEntity == null ? null : parentEntity.getName());
        response.setEntityValueAttribute(entityPO.getValueAttribute());
        response.setEntityValueType(entityPO.getValueType());
        response.setEntityIsCustomized(entityPO.checkIsCustomizedEntity());
        response.setEntityCreatedAt(entityPO.getCreatedAt());
        response.setEntityUpdatedAt(entityPO.getUpdatedAt());
        response.setEntityDescription(entityPO.getDescription());
        response.setDeviceGroup(deviceGroup);
        response.setWorkflowData(Optional.ofNullable(entityWorkflowMap.get(parentEntity == null ? entityPO.getId() : parentEntity.getId()))
                .map(workflowNameDTO -> new EntityWorkflowData(workflowNameDTO.getWorkflowId().toString(), workflowNameDTO.getName()))
                .orElse(null)
        );
        return response;
    }

    private List<String> searchAttachTargetIdsByKeyword(String keyword) {
        List<String> attachTargetIds = new ArrayList<>();
        if (!StringUtils.hasText(keyword)) {
            return attachTargetIds;
        }

        List<Integration> integrations = integrationServiceProvider.findIntegrations(
                f -> f.getName().toLowerCase().contains(keyword.toLowerCase()));
        if (integrations != null && !integrations.isEmpty()) {
            List<String> integrationIds = integrations.stream().map(Integration::getId).toList();
            attachTargetIds.addAll(integrationIds);
            attachTargetIds.addAll(getDeviceIdsByIntegrationId(integrationIds));
        }

        List<Long> deviceIdList = deviceFacade.fuzzySearchDeviceIdsByName(ComparisonOperator.CONTAINS, keyword);
        if (!deviceIdList.isEmpty()) {
            attachTargetIds.addAll(deviceIdList.stream().map(Object::toString).toList());
        }
        return attachTargetIds;
    }

    public List<EntityResponse> getChildren(Long entityId) {
        EntityPO parentEntityPO = entityRepository.findOneWithDataPermission(f -> f.eq(EntityPO.Fields.id, entityId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
        List<EntityPO> entityPOList = entityRepository.findAllWithDataPermission(f -> f.eq(EntityPO.Fields.parent, parentEntityPO.getKey()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return Collections.emptyList();
        }
        return convertEntityPOListToEntityResponses(entityPOList);
    }

    private List<EntityResponse> convertEntityPOListToEntityResponses(List<EntityPO> entityPOList) {
        return convertEntityPOPageToEntityResponses(new PageImpl<>(entityPOList)).toList();
    }

    private Page<EntityResponse> convertEntityPOListToFullEntityResponses(Page<EntityPO> entityPOPage) {
        if (entityPOPage == null || entityPOPage.isEmpty()) {
            return Page.empty();
        }

        val res = convertEntityPOPageToEntityResponses(entityPOPage);
        val entityIds = res.map(EntityResponse::getEntityId).map(Long::valueOf).toList();
        val entityIdToTags = entityTagService.entityIdToTags(entityIds);

        val entityKeys = res.stream()
                .map(EntityResponse::getEntityKey)
                .distinct()
                .collect(Collectors.toList());
        val entityKeyToLatestValue = entityValueService.findValuesByKeys(entityKeys);

        return res.map(entityResponse -> {
            entityResponse.setEntityTags(entityIdToTags.get(Long.valueOf(entityResponse.getEntityId())));
            entityResponse.setEntityLatestValue(Optional.ofNullable(entityKeyToLatestValue.get(entityResponse.getEntityKey()))
                    .map(value -> {
                        if (value instanceof Double doubleValue) {
                            return toDoubleString(doubleValue);
                        } else {
                            return value.toString();
                        }
                    })
                    .orElse(null));
            return entityResponse;
        });
    }

    public static String toDoubleString(double value) {
        return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
    }

    private Page<EntityResponse> convertEntityPOPageToEntityResponses(Page<EntityPO> entityPOPage) {
        List<String> relatedParentKeys = entityPOPage.stream()
                .map(EntityPO::getParent)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        List<EntityPO> relatedParentEntityPOList = new ArrayList<>();
        if (!relatedParentKeys.isEmpty()) {
            relatedParentEntityPOList.addAll(entityRepository.findAllWithDataPermission(f -> f.in(EntityPO.Fields.key, relatedParentKeys.toArray())));
        }
        Map<String, EntityPO> relatedParentKeyMap = new HashMap<>();
        if (!relatedParentEntityPOList.isEmpty()) {
            relatedParentKeyMap.putAll(relatedParentEntityPOList.stream().collect(Collectors.toMap(EntityPO::getKey, Function.identity())));
        }
        List<Long> foundDeviceIds = entityPOPage.stream()
                .filter(entityPO -> AttachTargetType.DEVICE.equals(entityPO.getAttachTarget()))
                .map(entityPO -> Long.parseLong(entityPO.getAttachTargetId()))
                .distinct()
                .toList();
        Map<String, DeviceNameDTO> deviceIdToDetails = deviceIdToDetails(foundDeviceIds);
        Map<Long, WorkflowNameDTO> entityWorkflowMap = new HashMap<>();
        workflowFacade.getWorkflowsByEntities(Stream.concat(relatedParentEntityPOList.stream(), entityPOPage.stream())
                .filter(entityPO -> entityPO.getType().equals(EntityType.SERVICE) && ObjectUtils.isEmpty(entityPO.getParent()))
                .map(EntityPO::getId)
                .distinct()
                .toList()
        ).forEach(workflowNameDTO -> entityWorkflowMap.put(workflowNameDTO.getEntityId(),  workflowNameDTO));
        Set<String> integrationIds = entityPOPage.stream()
                .filter(entityPO -> AttachTargetType.INTEGRATION.equals(entityPO.getAttachTarget()))
                .map(EntityPO::getAttachTargetId)
                .collect(Collectors.toSet());
        Map<String, Integration> integrationMap = integrationServiceProvider.findIntegrations(i -> integrationIds.contains(i.getId()))
                .stream()
                .collect(Collectors.toMap(Integration::getId, Function.identity(), (v1, v2) -> v1));
        Map<Long, DeviceGroupDTO> deviceGroupDTOMap = deviceGroupFacade.getGroupFromDeviceId(foundDeviceIds);
        return entityPOPage.map(entityPO -> convertEntityPOToEntityResponse(entityPO, integrationMap, deviceIdToDetails, relatedParentKeyMap, entityWorkflowMap, deviceGroupDTOMap));
    }

    public EntityMetaResponse getEntityMeta(Long entityId) {
        EntityPO entityPO = entityRepository.findOneWithDataPermission(filterable -> filterable.eq(EntityPO.Fields.id, entityId))
                .orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
        return convertEntityPOToEntityMetaResponse(entityPO);
    }

    public void updatePropertyEntity(UpdatePropertyEntityRequest updatePropertyEntityRequest) {
        Map<String, Object> exchange = updatePropertyEntityRequest.getExchange();
        List<String> entityKeys = exchange.keySet().stream().toList();
        List<EntityPO> entityPOList = entityRepository.findAllWithDataPermission(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            log.info("entity not found: {}", entityKeys);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
        List<String> nowEntityKeys = entityPOList.stream().map(EntityPO::getKey).toList();
        List<String> noInExchangeKeys = exchange.keySet().stream().filter(t -> !nowEntityKeys.contains(t)).toList();
        if (!noInExchangeKeys.isEmpty()) {
            noInExchangeKeys.forEach(exchange::remove);
        }
        entityPOList.forEach(entityPO -> {
            boolean isProperty = entityPO.getType().equals(EntityType.PROPERTY);
            if (!isProperty) {
                log.info("not property: {}", entityPO.getKey());
                exchange.remove(entityPO.getKey());
            }
            boolean isWritable = entityPO.getAccessMod() == AccessMod.RW || entityPO.getAccessMod() == AccessMod.W;
            if (!isWritable) {
                log.info("not writable: {}", entityPO.getKey());
                exchange.remove(entityPO.getKey());
            }
        });
        if (exchange.isEmpty()) {
            log.info("no property or writable entity found");
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
        ExchangePayload payload = new ExchangePayload(exchange);
        payload.validate();
        entityValueService.saveValuesAndPublishSync(payload);
    }

    public EventResponse serviceCall(ServiceCallRequest serviceCallRequest) {
        Map<String, Object> exchange = serviceCallRequest.getExchange();
        List<String> entityKeys = exchange.keySet().stream().toList();
        List<EntityPO> entityPOList = entityRepository.findAllWithDataPermission(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            log.info("entity not found: {}", entityKeys);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
        List<String> nowEntityKeys = entityPOList.stream().map(EntityPO::getKey).toList();
        List<String> noInExchangeKeys = exchange.keySet().stream().filter(t -> !nowEntityKeys.contains(t)).toList();
        if (!noInExchangeKeys.isEmpty()) {
            noInExchangeKeys.forEach(exchange::remove);
        }
        entityPOList.forEach(entityPO -> {
            boolean isService = entityPO.getType().equals(EntityType.SERVICE);
            if (!isService) {
                log.info("not service: {}", entityPO.getKey());
                exchange.remove(entityPO.getKey());
            }
        });
        if (exchange.isEmpty()) {
            log.info("no service found");
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
        ExchangePayload payload = new ExchangePayload(exchange);
        payload.validate();
        return entityValueService.saveValuesAndPublishSync(new ExchangePayload(exchange));
    }

    /**
     * Delete customized entities by ids.
     *
     * @param entityIds entity ids
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteCustomizedEntitiesByIds(List<Long> entityIds) {
        List<EntityPO> entityPOList = findEntityPOListAndTheirChildrenByIds(entityIds)
                .stream()
                // only customized entities allowed to be deleted
                .filter(EntityPO::checkIsCustomizedEntity)
                .toList();
        self().deleteEntitiesByPOList(entityPOList);
    }

    /**
     * Find entity PO list and their children by ids.
     *
     * @param entityIds entity ids
     * @return entity PO list
     */
    public List<EntityPO> findEntityPOListAndTheirChildrenByIds(List<Long> entityIds) {
        List<EntityPO> entityPOList = entityRepository.findAllById(entityIds);
        List<EntityPO> childrenEntityPOList = findChildrenEntityPOListByParents(entityPOList);

        return Stream.concat(entityPOList.stream(), childrenEntityPOList.stream())
                .toList();
    }

    private List<EntityPO> findChildrenEntityPOListByParents(List<EntityPO> entityPOList) {
        List<String> parentEntityKeys = entityPOList.stream()
                .filter(t -> t.getParent() == null)
                .map(EntityPO::getKey)
                .distinct()
                .toList();
        List<EntityPO> childrenEntityPOList = List.of();
        if (!parentEntityKeys.isEmpty()) {
            childrenEntityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.parent, parentEntityKeys.toArray()));
        }
        return childrenEntityPOList;
    }

    /**
     * Return a list of entities corresponding one-to-one with the given keys
     */
    private <T> List<Entity> mapKeysToEntities(List<Entity> entities, Collection<T> keys, Function<Entity, T> keyMapper) {
        Map<T, Entity> entityMap = entities.stream()
                .flatMap(entity -> entity.getChildren() == null
                        ? Stream.of(entity)
                        : Stream.concat(Stream.of(entity), entity.getChildren().stream()))
                .collect(Collectors.toMap(keyMapper, Function.identity(), (f, l) -> l));
        return keys.stream()
                .map(entityMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Find entity responses and their children by ids.
     *
     * @param entityIds entity ids
     * @return entity PO list
     */
    public List<EntityResponse> findEntityResponsesAndTheirChildrenByIds(List<Long> entityIds) {
        return convertEntityPOListToEntityResponses(findEntityPOListAndTheirChildrenByIds(entityIds));
    }

    /**
     * Update entity basic info.<br>
     * Currently only name and can be modified.
     *
     * @param entityId            entity ID
     * @param entityModifyRequest entity modify request
     */
    @Transactional(rollbackFor = Throwable.class)
    public void updateEntityBasicInfo(Long entityId, EntityModifyRequest entityModifyRequest) {
        if (!StringUtils.hasText(entityModifyRequest.getName()) && CollectionUtils.isEmpty(entityModifyRequest.getValueAttribute())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("name and valueAttribute can not be empty").build();
        }

        Entity entity = this.findById(entityId);
        if (entity == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("entity not found").build();
        }

        if (entityModifyRequest.getName() != null) {
            entity.setName(entityModifyRequest.getName());
        }

        // Only custom property entities' attribute can be updated
        if (
                !CollectionUtils.isEmpty(entityModifyRequest.getValueAttribute())
                && entity.getIntegrationId().equals(IntegrationConstants.SYSTEM_INTEGRATION_ID)
                && entity.getType().equals(EntityType.PROPERTY)
        ) {
            entity.setAttributes(entityModifyRequest.getValueAttribute());
        }

        this.batchSave(List.of(entity));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void createCustomEntity(EntityCreateRequest entityCreateRequest) {
        EntityBuilder entityBuilder = new EntityBuilder(IntegrationConstants.SYSTEM_INTEGRATION_ID)
                .id(SnowflakeUtil.nextId())
                .identifier(entityCreateRequest.getIdentifier())
                .valueType(entityCreateRequest.getValueType())
                .visible(entityCreateRequest.getVisible() == null || entityCreateRequest.getVisible())
                .attributes(entityCreateRequest.getValueAttribute());

        if (entityCreateRequest.getType().equals(EntityType.PROPERTY)) {
            entityBuilder.property(entityCreateRequest.getName(), entityCreateRequest.getAccessMod());
        } else {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Invalid custom type to create: " + entityCreateRequest.getType())
                    .build();
        }

        self().batchSave(List.of(entityBuilder.build()));
    }

    public List<EntityPO> listEntityPOById(List<Long> entityIds) {
        return entityRepository.findAllWithDataPermission(filterable -> filterable.in(EntityPO.Fields.id, toArray(entityIds)));
    }

    public Page<EntityResponse> advancedSearch(EntityAdvancedSearchQuery entityQuery) {

        boolean hasEntityCustomViewPermission = permissionFacade.hasMenuPermission(OperationPermissionCode.ENTITY_CUSTOM_VIEW);

        val columnToCondition = entityQuery.getEntityFilter();

        val integrationNameCondition = columnToCondition.get(EntitySearchColumn.INTEGRATION_NAME);
        val integrationIds = getIntegrationIdsByIntegrationNameCondition(integrationNameCondition);
        if (integrationIds != null && integrationIds.isEmpty()) {
            return Page.empty();
        }

        Set<String> intersectionDeviceIds = null;
        val attachTargetIds = new HashSet<>();
        if (integrationIds != null) {
            attachTargetIds.addAll(integrationIds);
            intersectionDeviceIds = new HashSet<>(getDeviceIdsByIntegrationId(integrationIds));
        }

        val deviceIdCondition = columnToCondition.get(EntitySearchColumn.DEVICE_ID);
        if (deviceIdCondition != null && deviceIdCondition.getOperator().equals(ComparisonOperator.EQ)) {
            if (CollectionUtils.isEmpty(deviceIdCondition.getValues())) {
                return Page.empty();
            }

            attachTargetIds.clear();
            if (intersectionDeviceIds != null) {
                intersectionDeviceIds = doIntersection(intersectionDeviceIds, List.of(deviceIdCondition.getValues().get(0)));
            } else {
                intersectionDeviceIds = new HashSet<>(List.of(deviceIdCondition.getValues().get(0)));
            }

            if (intersectionDeviceIds.isEmpty()) {
                return Page.empty();
            }
        }

        val deviceNameCondition = columnToCondition.get(EntitySearchColumn.DEVICE_NAME);
        val deviceIds = getDeviceIdsByDeviceNameCondition(deviceNameCondition);
        if (deviceIds != null) {
            if (deviceIds.isEmpty()) {
                return Page.empty();
            } else {
                intersectionDeviceIds = doIntersection(intersectionDeviceIds, deviceIds);
                if (intersectionDeviceIds.isEmpty()) {
                    return Page.empty();
                }
                attachTargetIds.clear();
            }
        }

        val deviceGroupNameCondition = columnToCondition.get(EntitySearchColumn.DEVICE_GROUP);
        val deviceIdsByGroupName = getDeviceIdsByDeviceGroupNameCondition(deviceGroupNameCondition);
        if (deviceIdsByGroupName != null) {
            if (deviceIdsByGroupName.isEmpty()) {
                return Page.empty();
            } else {
                intersectionDeviceIds = doIntersection(intersectionDeviceIds, deviceIdsByGroupName);
                if (intersectionDeviceIds.isEmpty()) {
                    return Page.empty();
                }
                attachTargetIds.clear();
            }
        }

        if (!CollectionUtils.isEmpty(intersectionDeviceIds)) {
            attachTargetIds.addAll(intersectionDeviceIds);
        }

        val entityParentCondition = columnToCondition.get(EntitySearchColumn.ENTITY_PARENT_NAME);
        val parentEntityKeys = getEntityIdsByEntityParentNameCondition(entityParentCondition);
        if (parentEntityKeys != null && parentEntityKeys.isEmpty()) {
            return Page.empty();
        }

        val tagCondition = columnToCondition.get(EntitySearchColumn.ENTITY_TAGS);
        val entityIds = getEntityIdsByEntityTagCondition(tagCondition);
        if (entityIds != null && entityIds.isEmpty()) {
            return Page.empty();
        }

        Consumer<Filterable> filterable = f -> {
            columnToCondition.forEach((column, condition) -> {
                if (!column.getSupportedOperators().contains(condition.getOperator())) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("Invalid operator").build();
                }
                if (column.getColumnName() != null) {
                    buildTextFilterable(f, condition.getOperator(), column.getColumnName(), condition.getValues());
                }
            });

            f.eq(EntityPO.Fields.visible, true);
            f.in(!CollectionUtils.isEmpty(entityIds), EntityPO.Fields.id, toArray(entityIds));
            f.in(!CollectionUtils.isEmpty(attachTargetIds), EntityPO.Fields.attachTargetId, toArray(attachTargetIds));
            f.ne(!hasEntityCustomViewPermission, EntityPO.Fields.attachTargetId, IntegrationConstants.SYSTEM_INTEGRATION_ID);

            if (entityParentCondition != null) {
                if (ComparisonOperator.IS_EMPTY.equals(entityParentCondition.getOperator())) {
                    f.isNull(EntityPO.Fields.parent);
                } else if (ComparisonOperator.IS_NOT_EMPTY.equals(entityParentCondition.getOperator())) {
                    f.isNotNull(EntityPO.Fields.parent);
                } else if (ComparisonOperator.NOT_CONTAINS.equals(entityParentCondition.getOperator())) {
                    f.or(f1 -> f1.in(!CollectionUtils.isEmpty(parentEntityKeys), EntityPO.Fields.parent, toArray(parentEntityKeys))
                            .isNull(EntityPO.Fields.parent));
                } else {
                    f.in(!CollectionUtils.isEmpty(parentEntityKeys), EntityPO.Fields.parent, toArray(parentEntityKeys));
                }
            }
        };

        val entityPOPage = entityRepository.findAllWithDataPermission(filterable, entityQuery.toPageable());
        return convertEntityPOListToFullEntityResponses(entityPOPage);
    }

    @Nullable
    private static <T> Object[] toArray(Collection<T> collection) {
        if (collection == null) {
            return null;
        }
        return collection.toArray();
    }

    @NonNull
    private static Set<String> doIntersection(Set<String> source, Collection<String> target) {
        if (source == null) {
            source = new HashSet<>(target);
        } else {
            source.retainAll(target);
        }
        return source;
    }

    @Nullable
    private List<Long> getEntityIdsByEntityTagCondition(EntityAdvancedSearchCondition tagCondition) {
        if (tagCondition == null) {
            return null;
        }

        return switch (tagCondition.getOperator()) {
            case EQ -> entityTagService.findEntityIdsByTagEquals(tagCondition.getValues());
            case CONTAINS -> entityTagService.findEntityIdsByTagContains(tagCondition.getValues());
            case NOT_CONTAINS -> entityTagService.findEntityIdsByTagNotContains(tagCondition.getValues());
            case ANY_EQUALS -> entityTagService.findEntityIdsByTagIn(tagCondition.getValues());
            case IS_EMPTY -> entityTagService.findEntityIdsByTagIsEmpty();
            case IS_NOT_EMPTY -> entityTagService.findEntityIdsByTagIsNotEmpty();
            default ->
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("Invalid operator").build();
        };
    }

    @Nullable
    private List<String> getEntityIdsByEntityParentNameCondition(EntityAdvancedSearchCondition entityParentCondition) {
        if (entityParentCondition == null || CollectionUtils.isEmpty(entityParentCondition.getValues())) {
            return null;
        }

        val operator = entityParentCondition.getOperator();
        if (ComparisonOperator.IS_EMPTY.equals(operator) || ComparisonOperator.IS_NOT_EMPTY.equals(operator)) {
            return null;
        }

        return entityRepository.findAllWithDataPermission(filterable -> {
                    buildTextFilterable(filterable, entityParentCondition.getOperator(), EntityPO.Fields.name, entityParentCondition.getValues());
                    filterable.isNull(EntityPO.Fields.parent);
                })
                .stream()
                .map(EntityPO::getKey)
                .collect(Collectors.toList());
    }

    @Nullable
    private List<String> getDeviceIdsByDeviceNameCondition(EntityAdvancedSearchCondition deviceNameCondition) {
        if (deviceNameCondition == null || CollectionUtils.isEmpty(deviceNameCondition.getValues())) {
            return null;
        }

        val keyword = deviceNameCondition.getValues().get(0);
        return deviceFacade.fuzzySearchDeviceIdsByName(deviceNameCondition.getOperator(), keyword)
                .stream()
                .map(String::valueOf)
                .toList();
    }

    @Nullable
    private List<String> getDeviceIdsByDeviceGroupNameCondition(EntityAdvancedSearchCondition deviceGroupNameCondition) {
        if (deviceGroupNameCondition == null || CollectionUtils.isEmpty(deviceGroupNameCondition.getValues())) {
            return null;
        }

        val operator = deviceGroupNameCondition.getOperator();
        if (!ComparisonOperator.ANY_EQUALS.equals(operator)) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("Unsupported operator: " + operator).build();
        }

        return deviceFacade.findDeviceIdsByGroupNameIn(deviceGroupNameCondition.getValues())
                .stream()
                .map(String::valueOf)
                .toList();
    }

    @Nullable
    private List<String> getIntegrationIdsByIntegrationNameCondition(EntityAdvancedSearchCondition integrationNameCondition) {
        if (integrationNameCondition == null || CollectionUtils.isEmpty(integrationNameCondition.getValues())) {
            return null;
        }
        val operator = integrationNameCondition.getOperator();
        val keyword = integrationNameCondition.getValues().get(0);
        return searchIntegrationIdsByName(operator, keyword);
    }

    @NonNull
    private List<String> getDeviceIdsByIntegrationId(List<String> integrationIds) {
        List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
        if (integrationDevices == null || integrationDevices.isEmpty()) {
            return Collections.emptyList();
        }

        return integrationDevices.stream()
                .map(DeviceNameDTO::getId)
                .map(String::valueOf)
                .toList();
    }

    @NonNull
    private List<String> searchIntegrationIdsByName(ComparisonOperator operator, String keyword) {
        return integrationServiceProvider.findIntegrations(integration -> switch (operator) {
                    case EQ -> Objects.equals(integration.getName(), keyword);
                    case NE -> !Objects.equals(integration.getName(), keyword);
                    case CONTAINS -> integration.getName().toLowerCase().contains(keyword.toLowerCase());
                    case NOT_CONTAINS -> !integration.getName().toLowerCase().contains(keyword.toLowerCase());
                    case START_WITH -> integration.getName().toLowerCase().startsWith(keyword.toLowerCase());
                    case END_WITH -> integration.getName().toLowerCase().endsWith(keyword.toLowerCase());
                    default ->
                            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("Unsupported operator: " + operator).build();
                })
                .stream()
                .map(Integration::getId)
                .toList();
    }

    private void buildTextFilterable(Filterable filterable, ComparisonOperator operator, String columnName, List<String> values) {
        switch (operator) {
            case CONTAINS ->
                    filterable.likeIgnoreCase(!CollectionUtils.isEmpty(values) && values.get(0) != null, columnName, values.get(0));
            case NOT_CONTAINS ->
                    filterable.notLikeIgnoreCase(!CollectionUtils.isEmpty(values) && values.get(0) != null, columnName, values.get(0));
            case START_WITH ->
                    filterable.startsWithIgnoreCase(!CollectionUtils.isEmpty(values) && values.get(0) != null, columnName, values.get(0));
            case END_WITH ->
                    filterable.endsWithIgnoreCase(!CollectionUtils.isEmpty(values) && values.get(0) != null, columnName, values.get(0));
            case EQ ->
                    filterable.eq(!CollectionUtils.isEmpty(values) && values.get(0) != null, columnName, values.get(0));
            case NE ->
                    filterable.ne(!CollectionUtils.isEmpty(values) && values.get(0) != null, columnName, values.get(0));
            case ANY_EQUALS -> filterable.in(!CollectionUtils.isEmpty(values), columnName, values.toArray());
            case IS_EMPTY -> filterable.isNull(columnName);
            case IS_NOT_EMPTY -> filterable.isNotNull(columnName);
            default ->
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("Unsupported operator: " + operator).build();
        }
    }

    public Map<String, Long> countEntityByTarget(AttachTargetType targetType, List<String> targetIds) {
        return entityRepository.countAndGroupByTargets(targetType, targetIds)
                .stream()
                .collect(Collectors.toMap(objects -> (String) objects[0], objects -> (Long) objects[1]));
    }

    private EntityService self() {
        return (EntityService) AopContext.currentProxy();
    }
}
