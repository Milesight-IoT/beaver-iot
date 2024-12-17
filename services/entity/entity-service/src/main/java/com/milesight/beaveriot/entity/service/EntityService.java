package com.milesight.beaveriot.entity.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.model.event.EntityEvent;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.entity.model.request.EntityCreateRequest;
import com.milesight.beaveriot.entity.model.request.EntityModifyRequest;
import com.milesight.beaveriot.entity.model.request.EntityQuery;
import com.milesight.beaveriot.entity.model.request.ServiceCallRequest;
import com.milesight.beaveriot.entity.model.request.UpdatePropertyEntityRequest;
import com.milesight.beaveriot.entity.model.response.EntityMetaResponse;
import com.milesight.beaveriot.entity.model.response.EntityResponse;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityHistoryRepository;
import com.milesight.beaveriot.entity.repository.EntityLatestRepository;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Lazy
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;

    @Autowired
    private IUserFacade userFacade;

    @Autowired
    private EventBus<EntityEvent> eventBus;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityHistoryRepository entityHistoryRepository;

    @Autowired
    private EntityLatestRepository entityLatestRepository;

    private static Entity convertPOToEntity(EntityPO entityPO, Map<String, DeviceNameDTO> deviceIdToDetails) {
        String integrationId = null;
        String deviceKey = null;
        String attachTargetId = entityPO.getAttachTargetId();
        AttachTargetType attachTarget = entityPO.getAttachTarget();
        if (attachTarget == AttachTargetType.DEVICE) {
            DeviceNameDTO deviceDetail = deviceIdToDetails.get(attachTargetId);
            if (deviceDetail != null) {
                deviceKey = deviceDetail.getKey();
                if (deviceDetail.getIntegrationConfig() != null) {
                    integrationId = deviceDetail.getIntegrationConfig().getId();
                }
            }
        } else if (attachTarget == AttachTargetType.INTEGRATION) {
            integrationId = attachTargetId;
        }
        return convertPOToEntity(entityPO, integrationId, deviceKey);
    }

    private static Entity convertPOToEntity(EntityPO entityPO, String IntegrationId, String deviceKey) {
        EntityBuilder entityBuilder = new EntityBuilder(IntegrationId, deviceKey)
                .id(entityPO.getId())
                .identifier(entityPO.getKey().substring(entityPO.getKey().lastIndexOf(".") + 1))
                .valueType(entityPO.getValueType())
                .attributes(entityPO.getValueAttribute());

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
        response.setValueAttribute(entityPO.getValueAttribute());
        response.setValueType(entityPO.getValueType());
        response.setCustomized(isCustomizedEntity(entityPO.getAttachTargetId()));
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
            Long entityId = null;
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
            entityPO.setParent(entity.getParentKey());
            entityPO.setAttachTarget(attachTarget);
            entityPO.setAttachTargetId(attachTargetId);
            entityPO.setValueAttribute(entity.getAttributes());
            entityPO.setValueType(entity.getValueType());
            entityPO.setVisible(entity.getVisible());
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
        HashMap<String, DeviceNameDTO> deviceIdToDetails = deviceIdToDetails(deviceIds);

        Map<String, List<Entity>> parentIdentifierToChildren = entityPOList.stream()
                .filter(entityPO -> StringUtils.hasText(entityPO.getParent()))
                .map(entityPO -> convertPOToEntity(entityPO, deviceIdToDetails))
                .collect(Collectors.groupingBy(Entity::getParentIdentifier));

        List<Entity> parentEntities = entityPOList.stream()
                .filter(entityPO -> !StringUtils.hasText(entityPO.getParent()))
                .map(entityPO -> {
                    Entity entity = convertPOToEntity(entityPO, deviceIdToDetails);
                    List<Entity> children = parentIdentifierToChildren.get(entity.getIdentifier());
                    if (children != null) {
                        entity.setChildren(children);
                        parentIdentifierToChildren.remove(entity.getIdentifier());
                    }
                    return entity;
                })
                .toList();

        return Stream.concat(parentEntities.stream(),
                        // include all children that have no parent
                        parentIdentifierToChildren.values().stream().flatMap(List::stream))
                .toList();
    }

    private HashMap<String, DeviceNameDTO> deviceIdToDetails(List<Long> deviceIds) {
        HashMap<String, DeviceNameDTO> deviceIdToDetails = new HashMap<>();
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
        List<Entity> allEntityList = new ArrayList<>();
        allEntityList.addAll(entityList);
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
            List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByKey(deviceKeys);
            if (deviceNameDTOList != null && !deviceNameDTOList.isEmpty()) {
                deviceKeyMap.putAll(deviceNameDTOList.stream().collect(Collectors.toMap(DeviceNameDTO::getKey, DeviceNameDTO::getId)));
            }
        }
        List<String> entityKeys = entityList.stream().map(Entity::getKey).filter(StringUtils::hasText).toList();
        List<EntityPO> dataEntityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        Map<String, EntityPO> dataEntityKeyMap = new HashMap<>();
        if (dataEntityPOList != null && !dataEntityPOList.isEmpty()) {
            dataEntityKeyMap.putAll(dataEntityPOList.stream().collect(Collectors.toMap(EntityPO::getKey, Function.identity())));
        }
        List<EntityPO> entityPOList = new ArrayList<>();
        entityList.forEach(t -> {
            EntityPO entityPO = saveConvert(userId, t, deviceKeyMap, dataEntityKeyMap);
            EntityPO dataEntityPO = dataEntityKeyMap.get(t.getKey());
            if (dataEntityPO == null
                    || dataEntityPO.getAccessMod() != entityPO.getAccessMod()
                    || dataEntityPO.getValueType() != entityPO.getValueType()
                    || !Objects.equals(JsonUtils.toJSON(dataEntityPO.getValueAttribute()), JsonUtils.toJSON(entityPO.getValueAttribute()))
                    || dataEntityPO.getType() != entityPO.getType()
                    || !dataEntityPO.getName().equals(entityPO.getName())) {
                entityPOList.add(entityPO);
            }
        });
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

        deleteEntitiesByPOList(entityPOList);
    }

    private void deleteEntitiesByPOList(List<EntityPO> entityPOList) {
        List<Long> entityIdList = entityPOList.stream().map(EntityPO::getId).toList();
        log.info("delete entities: {}", entityIdList);

        entityRepository.deleteAllById(entityIdList);
        entityHistoryRepository.deleteByEntityIds(entityIdList);
        entityLatestRepository.deleteByEntityIds(entityIdList);
        userFacade.deleteResource(ResourceType.ENTITY, entityIdList);

        List<Entity> entityList = convertPOListToEntities(entityPOList);
        entityList.forEach(entity ->
                eventBus.publish(EntityEvent.of(EntityEvent.EventType.DELETED, entity)));
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

    @Override
    public Entity findByKey(String entityKey) {
        Map<String, Entity> entityMap = findByKeys(entityKey);
        return entityMap.get(entityKey);
    }

    @Override
    public Map<String, Entity> findByKeys(String... entityKeys) {
        if (entityKeys == null || entityKeys.length == 0) {
            return new HashMap<>();
        }
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return convertPOListToEntities(entityPOList).stream()
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

        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.id, ids.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return convertPOListToEntities(entityPOList);
        } catch (Exception e) {
            log.error("find entity by ids error: {}", e.getMessage(), e);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

    public Page<EntityResponse> search(EntityQuery entityQuery) {
        boolean isExcludeChildren = entityQuery.getExcludeChildren() != null && entityQuery.getExcludeChildren();
        List<String> attachTargetIds = searchAttachTargetIdsByKeyword(entityQuery.getKeyword());

        Consumer<Filterable> filterable = f -> f.isNull(isExcludeChildren, EntityPO.Fields.parent)
                .in(entityQuery.getEntityType() != null && !entityQuery.getEntityType().isEmpty(), EntityPO.Fields.type, entityQuery.getEntityType() == null ? null : entityQuery.getEntityType().toArray())
                .in(entityQuery.getEntityIds() != null && !entityQuery.getEntityIds().isEmpty(), EntityPO.Fields.id, entityQuery.getEntityIds() == null ? null : entityQuery.getEntityIds().toArray())
                .in(entityQuery.getEntityValueType() != null && !entityQuery.getEntityValueType().isEmpty(), EntityPO.Fields.valueType, entityQuery.getEntityValueType() == null ? null : entityQuery.getEntityValueType().toArray())
                .in(entityQuery.getEntityAccessMod() != null && !entityQuery.getEntityAccessMod().isEmpty(), EntityPO.Fields.accessMod, entityQuery.getEntityAccessMod() == null ? null : entityQuery.getEntityAccessMod().toArray())
                .eq(Boolean.TRUE.equals(entityQuery.getCustomized()), EntityPO.Fields.attachTargetId, IntegrationConstants.SYSTEM_INTEGRATION_ID)
                .ne(Boolean.FALSE.equals(entityQuery.getCustomized()), EntityPO.Fields.attachTargetId, IntegrationConstants.SYSTEM_INTEGRATION_ID)
                .eq(!Boolean.TRUE.equals(entityQuery.getShowHidden()), EntityPO.Fields.visible, true)
                .or(f1 -> f1.likeIgnoreCase(StringUtils.hasText(entityQuery.getKeyword()), EntityPO.Fields.name, entityQuery.getKeyword())
                        .likeIgnoreCase(StringUtils.hasText(entityQuery.getKeyword()), EntityPO.Fields.key, entityQuery.getKeyword())
                        .in(!attachTargetIds.isEmpty(), EntityPO.Fields.attachTargetId, attachTargetIds.toArray()));
        Page<EntityPO> entityPOList = entityRepository.findAllWithDataPermission(filterable, entityQuery.toPageable());
        if (entityPOList == null || entityPOList.getContent().isEmpty()) {
            return Page.empty();
        }

        return convertEntityPOListToEntityResponses(entityPOList);
    }

    private EntityResponse convertEntityPOToEntityResponse(EntityPO entityPO, HashMap<String, DeviceNameDTO> deviceIdToDetails) {
        String deviceName = null;
        String integrationName = null;
        String attachTargetId = entityPO.getAttachTargetId();
        AttachTargetType attachTarget = entityPO.getAttachTarget();
        if (attachTarget == AttachTargetType.DEVICE) {
            DeviceNameDTO deviceDetail = deviceIdToDetails.get(attachTargetId);
            if (deviceDetail != null) {
                deviceName = deviceDetail.getName();
                if (deviceDetail.getIntegrationConfig() != null) {
                    integrationName = deviceDetail.getIntegrationConfig().getName();
                }
            }
        } else if (attachTarget == AttachTargetType.INTEGRATION) {
            Integration integration = integrationServiceProvider.getIntegration(attachTargetId);
            if (integration != null) {
                integrationName = integration.getName();
            }
        }

        EntityResponse response = new EntityResponse();
        response.setDeviceName(deviceName);
        response.setIntegrationName(integrationName);
        response.setEntityId(entityPO.getId().toString());
        response.setEntityAccessMod(entityPO.getAccessMod());
        response.setEntityKey(entityPO.getKey());
        response.setEntityType(entityPO.getType());
        response.setEntityName(entityPO.getName());
        response.setEntityValueAttribute(entityPO.getValueAttribute());
        response.setEntityValueType(entityPO.getValueType());
        response.setEntityIsCustomized(isCustomizedEntity(entityPO.getAttachTargetId()));
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

            List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
            if (integrationDevices != null && !integrationDevices.isEmpty()) {
                List<String> deviceIds = integrationDevices.stream().map(t -> String.valueOf(t.getId())).toList();
                attachTargetIds.addAll(deviceIds);
            }
        }

        List<DeviceNameDTO> deviceNameDTOList = deviceFacade.fuzzySearchDeviceByName(keyword);
        if (deviceNameDTOList != null && !deviceNameDTOList.isEmpty()) {
            List<String> deviceIds = deviceNameDTOList.stream().map(DeviceNameDTO::getId).map(String::valueOf).toList();
            attachTargetIds.addAll(deviceIds);
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
        return convertEntityPOListToEntityResponses(new PageImpl<>(entityPOList)).toList();
    }

    private Page<EntityResponse> convertEntityPOListToEntityResponses(Page<EntityPO> entityPOList) {
        List<Long> foundDeviceIds = entityPOList.stream()
                .filter(entityPO -> AttachTargetType.DEVICE.equals(entityPO.getAttachTarget()))
                .map(entityPO -> Long.parseLong(entityPO.getAttachTargetId()))
                .distinct()
                .toList();
        HashMap<String, DeviceNameDTO> deviceIdToDetails = deviceIdToDetails(foundDeviceIds);
        return entityPOList.map(entityPO -> convertEntityPOToEntityResponse(entityPO, deviceIdToDetails));
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
        exchangeFlowExecutor.syncExchangeDown(payload);
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
        return exchangeFlowExecutor.syncExchangeDown(payload);
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
                .filter(entityPO -> isCustomizedEntity(entityPO.getAttachTargetId()))
                .toList();
        deleteEntitiesByPOList(entityPOList);
    }

    /**
     * Find entity PO list and their children by ids.
     *
     * @param entityIds entity ids
     * @return entity PO list
     */
    public List<EntityPO> findEntityPOListAndTheirChildrenByIds(List<Long> entityIds) {
        List<EntityPO> entityPOList = entityRepository.findAllById(entityIds);
        List<Long> parentEntityIds = entityPOList.stream()
                .filter(t -> t.getParent() == null)
                .map(EntityPO::getId).toList();
        List<EntityPO> childrenEntityPOList = entityRepository.findAll(
                filter -> filter.in(EntityPO.Fields.parent, parentEntityIds.toArray()));
        return Stream.concat(entityPOList.stream(), childrenEntityPOList.stream())
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
     * @return entity metadata
     */
    @Transactional(rollbackFor = Throwable.class)
    public EntityMetaResponse updateEntityBasicInfo(Long entityId, EntityModifyRequest entityModifyRequest) {
        if (!StringUtils.hasText(entityModifyRequest.getName())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("name is empty").build();
        }

        EntityPO entityPO = entityRepository.findById(entityId)
                .orElseThrow(ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("entity not found")::build);
        entityPO.setName(entityModifyRequest.getName());
        entityRepository.save(entityPO);

        return convertEntityPOToEntityMetaResponse(entityPO);
    }

    @Transactional(rollbackFor = Throwable.class)
    public EntityMetaResponse createCustomizedEntity(EntityCreateRequest entityCreateRequest) {
        EntityPO entityPO = new EntityPO();
        entityPO.setName(entityCreateRequest.getName());
        entityPO.setType(entityCreateRequest.getType());
        entityPO.setAccessMod(entityCreateRequest.getAccessMod());
        entityPO.setValueAttribute(entityCreateRequest.getValueAttribute());
        entityPO.setValueType(entityCreateRequest.getValueType());
        entityPO.setParent(entityCreateRequest.getParentIdentifier());
        entityPO.setKey(entityCreateRequest.getIdentifier());
        entityPO.setVisible(entityCreateRequest.getVisible());
        entityPO.setTenantId(SecurityUserContext.getTenantId());
        entityPO.setUserId(SecurityUserContext.getUserId());
        entityPO.setAttachTarget(AttachTargetType.INTEGRATION);
        entityPO.setAttachTargetId(IntegrationConstants.SYSTEM_INTEGRATION_ID);
        entityPO = entityRepository.save(entityPO);
        return convertEntityPOToEntityMetaResponse(entityPO);
    }

    private static boolean isCustomizedEntity(String integrationId) {
        return IntegrationConstants.SYSTEM_INTEGRATION_ID.equals(integrationId);
    }

}
