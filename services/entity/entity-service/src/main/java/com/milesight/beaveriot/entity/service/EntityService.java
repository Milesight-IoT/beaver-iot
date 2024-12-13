package com.milesight.beaveriot.entity.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
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
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
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

/**
 * @author loong
 * @date 2024/10/16 14:22
 */
@Service
@Slf4j
public class EntityService implements EntityServiceProvider {

    @Autowired
    private EntityRepository entityRepository;
    @Autowired
    private EntityHistoryRepository entityHistoryRepository;
    @Autowired
    private EntityLatestRepository entityLatestRepository;
    @Autowired
    IDeviceFacade deviceFacade;
    @Lazy
    @Autowired
    IntegrationServiceProvider integrationServiceProvider;
    @Lazy
    @Autowired
    DeviceServiceProvider deviceServiceProvider;
    @Autowired
    ExchangeFlowExecutor exchangeFlowExecutor;
    @Autowired
    IUserFacade userFacade;
    @Autowired
    EventBus eventBus;

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
            return entityPO;
        } catch (Exception e) {
            log.error("save entity error:{}", e.getMessage(), e);
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

    @Override
    public List<Entity> findByTargetId(AttachTargetType targetType, String targetId) {
        if (!StringUtils.hasText(targetId)) {
            return new ArrayList<>();
        }
        return findByTargetIds(targetType, Collections.singletonList(targetId));
    }

    @Override
    public List<Entity> findByTargetIds(AttachTargetType targetType, List<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.attachTargetId, targetIds.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, String> deviceKeyMap = new HashMap<>();
        Map<String, Integration> deviceIntegrationMap = new HashMap<>();
        if (targetType == AttachTargetType.DEVICE) {
            List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIds(targetIds.stream().map(Long::valueOf).distinct().collect(Collectors.toList()));
            if (deviceNameDTOList != null && !deviceNameDTOList.isEmpty()) {
                deviceKeyMap.putAll(deviceNameDTOList.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), DeviceNameDTO::getKey)));
                deviceIntegrationMap.putAll(deviceNameDTOList.stream().filter(t -> t.getIntegrationConfig() != null).collect(Collectors.toMap(t -> String.valueOf(t.getId()), DeviceNameDTO::getIntegrationConfig)));
            }
        }
        List<EntityPO> parentEntityPOList = entityPOList.stream().filter(entityPO -> !StringUtils.hasText(entityPO.getParent())).toList();
        List<EntityPO> childrenEntityPOList = entityPOList.stream().filter(entityPO -> StringUtils.hasText(entityPO.getParent())).toList();
        List<Entity> childrenEntityList = new ArrayList<>();
        if (!childrenEntityPOList.isEmpty()) {
            childrenEntityPOList.forEach(childEntityPO -> {
                try {
                    String parentKey = childEntityPO.getParent();
                    if (StringUtils.hasText(parentKey)) {
                        parentKey = parentKey.substring(parentKey.lastIndexOf(".") + 1);
                    }
                    EntityBuilder entityBuilder = new EntityBuilder()
                            .id(childEntityPO.getId())
                            .identifier(childEntityPO.getKey().substring(childEntityPO.getKey().lastIndexOf(".") + 1))
                            .valueType(childEntityPO.getValueType())
                            .attributes(childEntityPO.getValueAttribute())
                            .parentIdentifier(parentKey);

                    Entity entity = null;
                    if (childEntityPO.getType() == EntityType.PROPERTY) {
                        entity = entityBuilder.property(childEntityPO.getName(), childEntityPO.getAccessMod())
                                .build();
                    } else if (childEntityPO.getType() == EntityType.SERVICE) {
                        entity = entityBuilder.service(childEntityPO.getName())
                                .build();
                    } else if (childEntityPO.getType() == EntityType.EVENT) {
                        entity = entityBuilder.event(childEntityPO.getName())
                                .build();
                    }
                    if (entity != null) {
                        childrenEntityList.add(entity);
                    }
                } catch (Exception e) {
                    log.error("find entity by targetId error:{}", e.getMessage(), e);
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                }
            });
        }
        List<Entity> entityList = new ArrayList<>();
        parentEntityPOList.forEach(entityPO -> {
            try {
                String parentKey = entityPO.getParent();
                if (StringUtils.hasText(parentKey)) {
                    parentKey = parentKey.substring(parentKey.lastIndexOf(".") + 1);
                }
                String identifier = entityPO.getKey().substring(entityPO.getKey().lastIndexOf(".") + 1);
                List<Entity> childEntityList = childrenEntityList.stream().filter(childEntity -> identifier.equals(childEntity.getParentIdentifier())).toList();

                String integrationId = null;
                String deviceKey = null;
                String attachTargetId = entityPO.getAttachTargetId();
                AttachTargetType attachTarget = entityPO.getAttachTarget();
                if (attachTarget == AttachTargetType.DEVICE) {
                    deviceKey = deviceKeyMap.get(attachTargetId);
                    Integration deviceIntegration = deviceIntegrationMap.get(attachTargetId);
                    if (deviceIntegration != null) {
                        integrationId = deviceIntegration.getId();
                    }
                } else if (attachTarget == AttachTargetType.INTEGRATION) {
                    integrationId = attachTargetId;
                }
                EntityBuilder entityBuilder = new EntityBuilder(integrationId, deviceKey)
                        .id(entityPO.getId())
                        .identifier(identifier)
                        .valueType(entityPO.getValueType())
                        .attributes(entityPO.getValueAttribute())
                        .parentIdentifier(parentKey)
                        .children(childEntityList);

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
                if (entity != null) {
                    entityList.add(entity);
                }
            } catch (Exception e) {
                log.error("find entity by targetId error:{}", e.getMessage(), e);
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
            }
        });
        return entityList;
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByTargetId(String targetId) {
        if (!StringUtils.hasText(targetId)) {
            return;
        }
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.eq(EntityPO.Fields.attachTargetId, targetId));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return;
        }
        List<Long> entityIdList = entityPOList.stream().map(EntityPO::getId).toList();
        List<Entity> entityList = findByTargetId(entityPOList.get(0).getAttachTarget(), targetId);

        entityRepository.deleteByTargetId(targetId);
        entityHistoryRepository.deleteByEntityIds(entityIdList);
        entityLatestRepository.deleteByEntityIds(entityIdList);

        userFacade.deleteResource(ResourceType.ENTITY, entityIdList);

        entityList.forEach(entity -> {
            eventBus.publish(EntityEvent.of(EntityEvent.EventType.DELETED, entity));
        });
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
        List<EntityPO> childrenEntityPOList = entityPOList.stream().filter(entityPO -> StringUtils.hasText(entityPO.getParent())).toList();
        List<Entity> childrenEntityList = new ArrayList<>();
        if (!childrenEntityPOList.isEmpty()) {
            childrenEntityPOList.forEach(childEntityPO -> {
                try {
                    String parentKey = childEntityPO.getParent();
                    if (StringUtils.hasText(parentKey)) {
                        parentKey = parentKey.substring(parentKey.lastIndexOf(".") + 1);
                    }

                    EntityBuilder entityBuilder = new EntityBuilder()
                            .id(childEntityPO.getId())
                            .identifier(childEntityPO.getKey().substring(childEntityPO.getKey().lastIndexOf(".") + 1))
                            .valueType(childEntityPO.getValueType())
                            .attributes(childEntityPO.getValueAttribute())
                            .parentIdentifier(parentKey);

                    Entity entity = null;
                    if (childEntityPO.getType() == EntityType.PROPERTY) {
                        entity = entityBuilder.property(childEntityPO.getName(), childEntityPO.getAccessMod())
                                .build();
                    } else if (childEntityPO.getType() == EntityType.SERVICE) {
                        entity = entityBuilder.service(childEntityPO.getName())
                                .build();
                    } else if (childEntityPO.getType() == EntityType.EVENT) {
                        entity = entityBuilder.event(childEntityPO.getName())
                                .build();
                    }
                    if (entity != null) {
                        childrenEntityList.add(entity);
                    }
                } catch (Exception e) {
                    log.error("find entity by targetId error:{}", e.getMessage(), e);
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                }
            });
        }
        List<Entity> entityList = new ArrayList<>();
        List<Long> deviceIds = entityPOList.stream().filter(entityPO -> entityPO.getAttachTarget() == AttachTargetType.DEVICE).map(t -> Long.valueOf(t.getAttachTargetId())).distinct().toList();
        Map<String, String> deviceKeyMap = new HashMap<>();
        Map<String, Integration> deviceIntegrationMap = new HashMap<>();
        if (!deviceIds.isEmpty()) {
            List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIds(deviceIds);
            if (deviceNameDTOList != null && !deviceNameDTOList.isEmpty()) {
                deviceKeyMap.putAll(deviceNameDTOList.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), DeviceNameDTO::getKey)));
                deviceIntegrationMap.putAll(deviceNameDTOList.stream().filter(t -> t.getIntegrationConfig() != null).collect(Collectors.toMap(t -> String.valueOf(t.getId()), DeviceNameDTO::getIntegrationConfig)));
            }
        }
        entityPOList.forEach(entityPO -> {
            try {
                String parentKey = entityPO.getParent();
                if (StringUtils.hasText(parentKey)) {
                    parentKey = parentKey.substring(parentKey.lastIndexOf(".") + 1);
                }
                String identifier = entityPO.getKey().substring(entityPO.getKey().lastIndexOf(".") + 1);
                List<Entity> childEntityList = childrenEntityList.stream().filter(childEntity -> identifier.equals(childEntity.getParentIdentifier())).toList();

                String integrationId = null;
                String deviceKey = null;
                String attachTargetId = entityPO.getAttachTargetId();
                AttachTargetType attachTarget = entityPO.getAttachTarget();
                if (attachTarget == AttachTargetType.DEVICE) {
                    deviceKey = deviceKeyMap.get(attachTargetId);

                    Integration deviceIntegration = deviceIntegrationMap.get(attachTargetId);
                    if (deviceIntegration != null) {
                        integrationId = deviceIntegration.getId();
                    }
                } else if (attachTarget == AttachTargetType.INTEGRATION) {
                    integrationId = attachTargetId;
                }
                EntityBuilder entityBuilder = new EntityBuilder(integrationId, deviceKey)
                        .id(entityPO.getId())
                        .identifier(identifier)
                        .valueType(entityPO.getValueType())
                        .attributes(entityPO.getValueAttribute())
                        .parentIdentifier(parentKey)
                        .children(childEntityList);

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
                if (entity != null) {
                    entityList.add(entity);
                }
            } catch (Exception e) {
                log.error("find entity by targetId error:{}", e.getMessage(), e);
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
            }
        });
        return entityList.stream().collect(Collectors.toMap(Entity::getKey, Function.identity()));
    }

    public Page<EntityResponse> search(EntityQuery entityQuery) {
        boolean isExcludeChildren = entityQuery.getExcludeChildren() != null && entityQuery.getExcludeChildren();
        List<String> attachTargetIds = new ArrayList<>();
        if (StringUtils.hasText(entityQuery.getKeyword())) {
            List<Integration> integrations = integrationServiceProvider.findIntegrations(f -> f.getName().toLowerCase().contains(entityQuery.getKeyword().toLowerCase()));
            if (integrations != null && !integrations.isEmpty()) {
                List<String> integrationIds = integrations.stream().map(Integration::getId).toList();
                attachTargetIds.addAll(integrationIds);
                List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
                if (integrationDevices != null && !integrationDevices.isEmpty()) {
                    List<String> deviceIds = integrationDevices.stream().map(t -> String.valueOf(t.getId())).toList();
                    attachTargetIds.addAll(deviceIds);
                }
            }
            List<DeviceNameDTO> deviceNameDTOList = deviceFacade.fuzzySearchDeviceByName(entityQuery.getKeyword());
            if (deviceNameDTOList != null && !deviceNameDTOList.isEmpty()) {
                List<String> deviceIds = deviceNameDTOList.stream().map(DeviceNameDTO::getId).map(String::valueOf).toList();
                attachTargetIds.addAll(deviceIds);
            }
        }
        Consumer<Filterable> filterable = f -> f.in(entityQuery.getEntityType() != null && !entityQuery.getEntityType().isEmpty(), EntityPO.Fields.type, entityQuery.getEntityType() == null ? null : entityQuery.getEntityType().toArray())
                .isNull(isExcludeChildren, EntityPO.Fields.parent)
                .in(entityQuery.getEntityValueType() != null && !entityQuery.getEntityValueType().isEmpty(), EntityPO.Fields.valueType, entityQuery.getEntityValueType() == null ? null : entityQuery.getEntityValueType().toArray())
                .in(entityQuery.getEntityAccessMod() != null && !entityQuery.getEntityAccessMod().isEmpty(), EntityPO.Fields.accessMod, entityQuery.getEntityAccessMod() == null ? null : entityQuery.getEntityAccessMod().toArray())
                .or(f1 -> f1.likeIgnoreCase(StringUtils.hasText(entityQuery.getKeyword()), EntityPO.Fields.name, entityQuery.getKeyword())
                        .in(!attachTargetIds.isEmpty(), EntityPO.Fields.attachTargetId, attachTargetIds.toArray()));
        Page<EntityPO> entityPOList = entityRepository.findAllWithDataPermission(filterable, entityQuery.toPageable());
        if (entityPOList == null || entityPOList.getContent().isEmpty()) {
            return Page.empty();
        }
        List<Long> resultDeviceIds = entityPOList.stream().filter(entityPO -> entityPO.getAttachTarget() == AttachTargetType.DEVICE).map(t -> Long.parseLong(t.getAttachTargetId())).distinct().toList();
        List<DeviceNameDTO> resultDevices = deviceFacade.getDeviceNameByIds(resultDeviceIds);
        Map<String, String> deviceNameMap = new HashMap<>();
        Map<String, String> deviceIntegrationNameMap = new HashMap<>();
        if (resultDevices != null && !resultDevices.isEmpty()) {
            deviceNameMap.putAll(resultDevices.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), DeviceNameDTO::getName)));
            deviceIntegrationNameMap.putAll(resultDevices.stream().filter(t -> t.getIntegrationConfig() != null).collect(Collectors.toMap(t -> String.valueOf(t.getId()), t -> t.getIntegrationConfig().getName())));
        }
        return entityPOList.map(entityPO -> {
            EntityResponse response = new EntityResponse();
            String deviceName = null;
            String integrationName = null;
            if (entityPO.getAttachTarget() == AttachTargetType.DEVICE) {
                String deviceId = entityPO.getAttachTargetId();
                deviceName = deviceNameMap.get(deviceId);
                integrationName = deviceIntegrationNameMap.get(deviceId);
            } else if (entityPO.getAttachTarget() == AttachTargetType.INTEGRATION) {
                String integrationId = entityPO.getAttachTargetId();
                Integration integration = integrationServiceProvider.getIntegration(integrationId);
                if (integration != null) {
                    integrationName = integration.getName();
                }
            }
            response.setDeviceName(deviceName);
            response.setIntegrationName(integrationName);
            response.setEntityId(entityPO.getId().toString());
            response.setEntityAccessMod(entityPO.getAccessMod());
            response.setEntityKey(entityPO.getKey());
            response.setEntityType(entityPO.getType());
            response.setEntityName(entityPO.getName());
            response.setEntityValueAttribute(entityPO.getValueAttribute());
            response.setEntityValueType(entityPO.getValueType().name());
            return response;
        });
    }

    public List<EntityResponse> getChildren(Long entityId) {
        EntityPO entityPO = entityRepository.findOneWithDataPermission(f -> f.eq(EntityPO.Fields.id, entityId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
        List<EntityPO> entityPOList = entityRepository.findAllWithDataPermission(f -> f.eq(EntityPO.Fields.parent, entityPO.getKey()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> resultDeviceIds = entityPOList.stream().filter(t -> t.getAttachTarget() == AttachTargetType.DEVICE).map(t -> Long.parseLong(t.getAttachTargetId())).distinct().toList();
        List<DeviceNameDTO> resultDevices = deviceFacade.getDeviceNameByIds(resultDeviceIds);
        Map<String, String> deviceNameMap = new HashMap<>();
        Map<String, String> deviceIntegrationNameMap = new HashMap<>();
        if (resultDevices != null && !resultDevices.isEmpty()) {
            deviceNameMap.putAll(resultDevices.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), DeviceNameDTO::getName)));
            deviceIntegrationNameMap.putAll(resultDevices.stream().filter(t -> t.getIntegrationConfig() != null).collect(Collectors.toMap(t -> String.valueOf(t.getId()), t -> t.getIntegrationConfig().getName())));
        }
        return entityPOList.stream().map(t -> {
            EntityResponse response = new EntityResponse();
            String deviceName = null;
            String integrationName = null;
            if (t.getAttachTarget() == AttachTargetType.DEVICE) {
                String deviceId = t.getAttachTargetId();
                deviceName = deviceNameMap.get(deviceId);
                integrationName = deviceIntegrationNameMap.get(deviceId);
            } else if (t.getAttachTarget() == AttachTargetType.INTEGRATION) {
                String integrationId = t.getAttachTargetId();
                Integration integration = integrationServiceProvider.getIntegration(integrationId);
                if (integration != null) {
                    integrationName = integration.getName();
                }
            }
            response.setDeviceName(deviceName);
            response.setIntegrationName(integrationName);
            response.setEntityId(t.getId().toString());
            response.setEntityAccessMod(t.getAccessMod());
            response.setEntityKey(t.getKey());
            response.setEntityType(t.getType());
            response.setEntityName(t.getName());
            response.setEntityValueAttribute(t.getValueAttribute());
            response.setEntityValueType(t.getValueType().name());
            return response;
        }).collect(Collectors.toList());
    }

    public EntityMetaResponse getEntityMeta(Long entityId) {
        EntityPO entityPO = entityRepository.findOneWithDataPermission(filterable -> filterable.eq(EntityPO.Fields.id, entityId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
        EntityMetaResponse entityMetaResponse = new EntityMetaResponse();
        entityMetaResponse.setKey(entityPO.getKey());
        entityMetaResponse.setName(entityPO.getName());
        entityMetaResponse.setType(entityPO.getType());
        entityMetaResponse.setAccessMod(entityPO.getAccessMod());
        entityMetaResponse.setValueAttribute(entityPO.getValueAttribute());
        entityMetaResponse.setValueType(entityPO.getValueType());
        return entityMetaResponse;
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

}
