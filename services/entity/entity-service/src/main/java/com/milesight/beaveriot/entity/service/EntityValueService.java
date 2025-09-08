package com.milesight.beaveriot.entity.service;

import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.context.integration.GenericExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.proxy.MapExchangePayloadProxy;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.entity.enums.AggregateType;
import com.milesight.beaveriot.entity.model.dto.EntityHistoryUnionQuery;
import com.milesight.beaveriot.entity.model.request.EntityAggregateQuery;
import com.milesight.beaveriot.entity.model.request.EntityHistoryQuery;
import com.milesight.beaveriot.entity.model.response.EntityAggregateResponse;
import com.milesight.beaveriot.entity.model.response.EntityHistoryResponse;
import com.milesight.beaveriot.entity.model.response.EntityLatestResponse;
import com.milesight.beaveriot.entity.po.EntityHistoryPO;
import com.milesight.beaveriot.entity.po.EntityLatestPO;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityHistoryRepository;
import com.milesight.beaveriot.entity.repository.EntityLatestRepository;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import jakarta.persistence.EntityManager;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/11/1 9:22
 */
@Service
@Slf4j
public class EntityValueService implements EntityValueServiceProvider {

    @Autowired
    private EntityRepository entityRepository;
    @Autowired
    private EntityHistoryRepository entityHistoryRepository;
    @Autowired
    private EntityLatestRepository entityLatestRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private GenericExchangeFlowExecutor genericExchangeFlowExecutor;

    private final Comparator<byte[]> byteArrayComparator = (a, b) -> {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int cmp = Byte.compare(a[i], b[i]);
            if (cmp != 0) return cmp;
        }
        return Integer.compare(a.length, b.length);
    };

    @Override
    public void saveValuesAndPublishAsync(ExchangePayload exchangePayload) {
        genericExchangeFlowExecutor.saveValuesAndPublishAsync(exchangePayload);
    }

    @Override
    public EventResponse saveValuesAndPublishSync(ExchangePayload exchangePayload) {
        return genericExchangeFlowExecutor.saveValuesAndPublishSync(exchangePayload);
    }

    @Override
    public void saveValuesAndPublishAsync(ExchangePayload exchangePayload, String eventType) {
        genericExchangeFlowExecutor.saveValuesAndPublishAsync(exchangePayload, eventType);
    }

    @Override
    public EventResponse saveValuesAndPublishSync(ExchangePayload exchangePayload, String eventType) {
        return genericExchangeFlowExecutor.saveValuesAndPublishSync(exchangePayload, eventType);
    }

    @Override
    public void saveValues(ExchangePayload exchange, long timestamp) {
        // Save event entities， only save history
        Map<String, Object> eventEntities = exchange.getPayloadsByEntityType(EntityType.EVENT);
        if (!ObjectUtils.isEmpty(eventEntities)) {
            saveHistoryRecord(eventEntities, timestamp);
        }

        // Save property entities
        Map<String, Object> propertyEntities = exchange.getPayloadsByEntityType(EntityType.PROPERTY);
        if (!ObjectUtils.isEmpty(propertyEntities)) {
            saveLatestValues(ExchangePayload.create(propertyEntities));
            saveHistoryRecord(propertyEntities, timestamp);
        }

        // Save service entities， only save history
        Map<String, Object> serviceEntities = exchange.getPayloadsByEntityType(EntityType.SERVICE);
        if (!ObjectUtils.isEmpty(serviceEntities)) {
            saveHistoryRecord(serviceEntities, timestamp);
        }
    }

    @Override
    public void saveValues(ExchangePayload exchangePayload) {
        saveValues(exchangePayload, exchangePayload.getTimestamp());
    }

    @Override
    public void saveLatestValues(ExchangePayload values) {
        self().saveLatestValues(values, System.currentTimeMillis());
    }

    @BatchCacheEvict(cacheNames = CacheKeyConstants.ENTITY_LATEST_VALUE_CACHE_NAME, key = "#result", keyPrefix = CacheKeyConstants.TENANT_PREFIX)
    public List<String> saveLatestValues(Map<String, Object> values, long timestamp) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> entityKeys = values.keySet().stream().toList();
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return List.of();
        }
        Map<String, EntityPO> entityKeyMap = entityPOList.stream().collect(Collectors.toMap(EntityPO::getKey, Function.identity()));
        List<Long> entityIds = entityPOList.stream().map(EntityPO::getId).toList();
        List<EntityLatestPO> nowEntityLatestPOList = entityLatestRepository.findAll(filter -> filter.in(EntityLatestPO.Fields.entityId, entityIds.toArray()));
        Map<Long, EntityLatestPO> entityIdDataMap = new HashMap<>();
        if (nowEntityLatestPOList != null && !nowEntityLatestPOList.isEmpty()) {
            entityIdDataMap.putAll(nowEntityLatestPOList.stream().collect(Collectors.toMap(EntityLatestPO::getEntityId, Function.identity())));
        }
        List<EntityLatestPO> entityLatestPOList = new ArrayList<>();
        values.forEach((entityKey, payload) -> {
            EntityPO entityPO = entityKeyMap.get(entityKey);
            if (entityPO == null) {
                return;
            }
            Long entityId = entityPO.getId();
            EntityValueType entityValueType = entityPO.getValueType();
            EntityLatestPO dataEntityLatest = entityIdDataMap.get(entityId);
            Long entityLatestId = null;
            if (dataEntityLatest == null) {
                entityLatestId = SnowflakeUtil.nextId();
            } else {
                if (dataEntityLatest.getTimestamp() >= timestamp) {
                    log.info("entity latest data is bigger than exchangePayload timestamp, entityId:{}, exchangePayload:{}", entityId, values);
                    return;
                }
                entityLatestId = dataEntityLatest.getId();
            }
            EntityLatestPO entityLatestPO = new EntityLatestPO();
            entityLatestPO.setId(entityLatestId);
            entityLatestPO.setEntityId(entityId);
            entityLatestPO.setValue(entityValueType, payload);
            entityLatestPO.setTimestamp(timestamp);
            entityLatestPOList.add(entityLatestPO);
        });
        entityLatestRepository.saveAll(entityLatestPOList);
        return entityKeys;
    }

    @Override
    public void saveHistoryRecord(Map<String, Object> recordValues) {
        saveHistoryRecord(recordValues, System.currentTimeMillis());
    }

    @Override
    public void saveHistoryRecord(Map<String, Object> recordValues, long timestamp) {
        doSaveHistoryRecord(recordValues, timestamp, false);
    }

    @Override
    public void mergeHistoryRecord(Map<String, Object> recordValues, long timestamp) {
        doSaveHistoryRecord(recordValues, timestamp, true);
    }

    private void doSaveHistoryRecord(Map<String, Object> recordValues, long timestamp, boolean isMerge) {
        if (recordValues == null || recordValues.isEmpty()) {
            return;
        }
        List<String> entityKeys = recordValues.keySet().stream().toList();
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return;
        }
        Map<String, EntityPO> entityKeyMap = entityPOList.stream().collect(Collectors.toMap(EntityPO::getKey, Function.identity()));
        Map<String, EntityHistoryPO> keyToExistingHistoryRecord = new HashMap<>();
        if (isMerge) {
            Map<String, Long> entityKeyToId = entityPOList.stream()
                    .collect(Collectors.toMap(EntityPO::getKey, EntityPO::getId, (a, b) -> a));
            keyToExistingHistoryRecord.putAll(getExistingEntityHistoryMap(entityKeyToId, timestamp));
        }
        List<EntityHistoryPO> entityHistoryPOList = new ArrayList<>();
        recordValues.forEach((entityKey, payload) -> {
            EntityPO entityPO = entityKeyMap.get(entityKey);
            if (entityPO == null) {
                return;
            }
            Long entityId = entityPO.getId();
            EntityValueType entityValueType = entityPO.getValueType();
            EntityHistoryPO entityHistoryPO = new EntityHistoryPO();
            EntityHistoryPO dataHistory = keyToExistingHistoryRecord.get(entityKey);
            Long historyId = null;
            String operatorId = SecurityUserContext.getUserId() == null ? null : SecurityUserContext.getUserId().toString();
            if (dataHistory == null) {
                historyId = SnowflakeUtil.nextId();
                entityHistoryPO.setCreatedBy(operatorId);
            } else {
                historyId = dataHistory.getId();
                entityHistoryPO.setCreatedAt(dataHistory.getCreatedAt());
                entityHistoryPO.setCreatedBy(dataHistory.getCreatedBy());
            }
            entityHistoryPO.setId(historyId);
            entityHistoryPO.setEntityId(entityId);
            entityHistoryPO.setValue(entityValueType, payload);
            entityHistoryPO.setTimestamp(timestamp);
            entityHistoryPO.setUpdatedBy(operatorId);
            entityHistoryPOList.add(entityHistoryPO);
        });
        entityHistoryRepository.saveAll(entityHistoryPOList);
    }

    private Map<String, EntityHistoryPO> getExistingEntityHistoryMap(Map<String, Long> entityKeyToId, long timestamp) {
        List<EntityHistoryUnionQuery> entityHistoryUnionQueryList = entityKeyToId.keySet().stream()
                .map(o -> {
                    Long entityId = entityKeyToId.get(o);
                    if (entityId == null) {
                        return null;
                    }
                    EntityHistoryUnionQuery entityHistoryUnionQuery = new EntityHistoryUnionQuery();
                    entityHistoryUnionQuery.setEntityId(entityId);
                    entityHistoryUnionQuery.setTimestamp(timestamp);
                    return entityHistoryUnionQuery;
                })
                .filter(Objects::nonNull)
                .toList();
        Map<Long, String> entityIdToKey = entityKeyToId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (a, b) -> a));
        List<EntityHistoryPO> existEntityHistoryPOList = entityHistoryRepository.findByUnionUnique(entityManager, entityHistoryUnionQueryList);
        if (existEntityHistoryPOList != null && !existEntityHistoryPOList.isEmpty()) {
            return new HashMap<>(existEntityHistoryPOList.stream()
                    .collect(Collectors.toMap(entityHistoryPO -> entityIdToKey.get(entityHistoryPO.getEntityId()), Function.identity(), (a, b) -> a)));
        }
        return Collections.emptyMap();
    }

    /**
     * Check if the entity history records exist
     *
     * @param keys      Entity keys of the history record
     * @param timestamp Timestamp of the history record
     * @return Entity keys of the existing history record
     */
    @Override
    public Set<String> existHistoryRecord(Set<String> keys, long timestamp) {
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, keys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return Collections.emptySet();
        }
        Map<String, Long> entityKeyToId = entityPOList.stream()
                .collect(Collectors.toMap(EntityPO::getKey, EntityPO::getId, (a, b) -> a));
        return getExistingEntityHistoryMap(entityKeyToId, timestamp).keySet();
    }

    @Override
    public boolean existHistoryRecord(String key, long timestamp) {
        List<EntityHistoryUnionQuery> entityHistoryUnionQueryList = new ArrayList<>();

        EntityPO entityPO = entityRepository.findOne(filter -> filter.eq(EntityPO.Fields.key, key)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("entity not found").build());
        Long entityId = entityPO.getId();
        EntityHistoryUnionQuery entityHistoryUnionQuery = new EntityHistoryUnionQuery();
        entityHistoryUnionQuery.setEntityId(entityId);
        entityHistoryUnionQuery.setTimestamp(timestamp);
        entityHistoryUnionQueryList.add(entityHistoryUnionQuery);

        List<EntityHistoryPO> existEntityHistoryPOList = entityHistoryRepository.findByUnionUnique(entityManager, entityHistoryUnionQueryList);
        return existEntityHistoryPOList != null && !existEntityHistoryPOList.isEmpty();
    }

    @Override
    public Object findValueByKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return findValuesByKeys(Collections.singletonList(key)).get(key);
    }

    @Data
    public static class EntityLatestValueCache {
        private Object value;

        private EntityValueType valueType;

        private Long updatedAt;
    }

    @Override
    @NonNull
    public Map<String, Object> findValuesByKeys(List<String> keys) {
        Map<String, Object> resultMap = new HashMap<>();
        List<EntityPO> allEntities = new ArrayList<>();
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, keys.toArray()));
        List<EntityPO> childrenEntities = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.parent, keys.toArray()));
        if (entityPOList != null && !entityPOList.isEmpty()) {
            allEntities.addAll(entityPOList);
        }
        if (childrenEntities != null && !childrenEntities.isEmpty()) {
            allEntities.addAll(childrenEntities);
        }
        if (allEntities.isEmpty()) {
            return resultMap;
        }

        allEntities = new ArrayList<>(allEntities.stream()
                .collect(Collectors.toMap(EntityPO::getId, Function.identity(), (a, b) -> a))
                .values());
        List<EntityLatestValueCache> lvList = self().findSpecificEntityValue(allEntities);
        for (int i = 0; i < allEntities.size(); i++) {
            EntityLatestValueCache lv = lvList.get(i);
            if (lv.getValue() != null) {
                resultMap.put(allEntities.get(i).getKey(), lv.getValue());
            }
        }

        return resultMap;
    }

    @BatchCacheable(cacheNames = CacheKeyConstants.ENTITY_LATEST_VALUE_CACHE_NAME, key = "#p0.![key]", keyPrefix = CacheKeyConstants.TENANT_PREFIX)
    public List<EntityLatestValueCache> findSpecificEntityValue(List<EntityPO> entityList) {
        Map<Long, EntityLatestValueCache> entityIdToValue = new LinkedHashMap<>();
        entityList.forEach(entityPO -> entityIdToValue.put(entityPO.getId(), new EntityLatestValueCache()));
        List<EntityLatestPO> entityLatestPOList = entityLatestRepository.findAll(filter -> filter.in(EntityLatestPO.Fields.entityId, entityIdToValue.keySet().toArray()));
        if (entityLatestPOList == null || entityLatestPOList.isEmpty()) {
            return entityIdToValue.values().stream().toList();
        }

        entityLatestPOList.forEach(entityLatestPO -> {
            EntityLatestValueCache value = entityIdToValue.get(entityLatestPO.getEntityId());
            EntityLatestValueCache nValue = convertEntityLatestCache(entityLatestPO);
            value.setValueType(nValue.getValueType());
            value.setValue(nValue.getValue());
            value.setUpdatedAt(nValue.getUpdatedAt());
        });

        return entityIdToValue.values().stream().toList();
    }

    private EntityLatestValueCache convertEntityLatestCache(EntityLatestPO entityLatestPO) {
        EntityLatestValueCache lv = new EntityLatestValueCache();

        Object value = null;
        EntityValueType valueType = null;
        if (entityLatestPO.getValueBoolean() != null) {
            value = entityLatestPO.getValueBoolean();
            valueType = EntityValueType.BOOLEAN;
        } else if (entityLatestPO.getValueLong() != null) {
            value = entityLatestPO.getValueLong();
            valueType = EntityValueType.LONG;
        } else if (entityLatestPO.getValueDouble() != null) {
            value = entityLatestPO.getValueDouble();
            valueType = EntityValueType.DOUBLE;
        } else if (entityLatestPO.getValueString() != null) {
            value = entityLatestPO.getValueString();
            valueType = EntityValueType.STRING;
        } else if (entityLatestPO.getValueBinary() != null) {
            value = entityLatestPO.getValueBinary();
            valueType = EntityValueType.BINARY;
        }

        lv.setValue(value);
        lv.setValueType(valueType);
        lv.setUpdatedAt(entityLatestPO.getUpdatedAt());
        return lv;
    }

    @Override
    @NonNull
    public <T extends ExchangePayload> T findValuesByKey(String key, Class<T> entitiesClazz) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("key is empty");
        }
        Map<String, Object> exchangeValues = findValuesByKeys(Collections.singletonList(key));
        return new MapExchangePayloadProxy<>(exchangeValues, entitiesClazz).proxy();
    }

    public Page<EntityHistoryResponse> historySearch(EntityHistoryQuery entityHistoryQuery) {

        if (entityHistoryQuery.getStartTimestamp() == null) {
            entityHistoryQuery.setStartTimestamp(0L);
        }
        if (entityHistoryQuery.getEndTimestamp() == null) {
            entityHistoryQuery.setEndTimestamp(System.currentTimeMillis());
        }
        if (entityHistoryQuery.getEndTimestamp() <= entityHistoryQuery.getStartTimestamp()) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED)
                    .detailMessage("startTimestamp should be less than endTimestamp")
                    .build();
        }

        List<Long> entityIds = entityHistoryQuery.getEntityIds() == null
                ? new ArrayList<>()
                : entityHistoryQuery.getEntityIds()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (entityHistoryQuery.getEntityId() != null) {
            entityIds.add(entityHistoryQuery.getEntityId());
        }

        if (entityHistoryQuery.getSort().getOrders().isEmpty()) {
            entityHistoryQuery.sort(new Sorts().desc(EntityHistoryPO.Fields.timestamp));
        }

        List<Long> entityIdsWithPermission = entityRepository.findAllWithDataPermission(f -> f.in(EntityPO.Fields.id, entityIds.toArray()))
                .stream().map(EntityPO::getId).toList();
        if (CollectionUtils.isEmpty(entityIdsWithPermission)) {
            return Page.empty();
        }

        Page<EntityHistoryPO> entityHistoryPage = entityHistoryRepository.findByEntityIdInAndTimestampBetween(
                entityIdsWithPermission,
                entityHistoryQuery.getStartTimestamp(),
                entityHistoryQuery.getEndTimestamp(),
                entityHistoryQuery.toPageable()
        );
        if (entityHistoryPage == null || entityHistoryPage.getContent().isEmpty()) {
            return Page.empty();
        }

        return entityHistoryPage.map(entityHistoryPO -> {
            EntityHistoryResponse response = new EntityHistoryResponse();
            response.setId(entityHistoryPO.getId().toString());
            response.setEntityId(entityHistoryPO.getEntityId().toString());
            response.setTimestamp(entityHistoryPO.getTimestamp().toString());
            if (entityHistoryPO.getValueBoolean() != null) {
                response.setValue(entityHistoryPO.getValueBoolean());
                response.setValueType(EntityValueType.BOOLEAN);
            } else if (entityHistoryPO.getValueLong() != null) {
                response.setValue(entityHistoryPO.getValueLong().toString());
                response.setValueType(EntityValueType.LONG);
            } else if (entityHistoryPO.getValueDouble() != null) {
                response.setValue(entityHistoryPO.getValueDouble());
                response.setValueType(EntityValueType.DOUBLE);
            } else if (entityHistoryPO.getValueString() != null) {
                response.setValue(entityHistoryPO.getValueString());
                response.setValueType(EntityValueType.STRING);
            } else if (entityHistoryPO.getValueBinary() != null) {
                response.setValue(entityHistoryPO.getValueBinary());
                response.setValueType(EntityValueType.BINARY);
            }
            return response;
        });
    }

    public EntityAggregateResponse historyAggregate(EntityAggregateQuery entityAggregateQuery) {
        EntityAggregateResponse entityAggregateResponse = new EntityAggregateResponse();
        Long entityIdWithPermission = entityRepository.findOneWithDataPermission(f -> f.eq(EntityPO.Fields.id, entityAggregateQuery.getEntityId()))
                .map(EntityPO::getId).orElse(null);
        if (entityIdWithPermission == null) {
            return entityAggregateResponse;
        }

        List<EntityHistoryPO> entityHistoryPOList = entityHistoryRepository.findAll(filter -> filter.eq(EntityHistoryPO.Fields.entityId, entityIdWithPermission)
                        .ge(EntityHistoryPO.Fields.timestamp, entityAggregateQuery.getStartTimestamp())
                        .le(EntityHistoryPO.Fields.timestamp, entityAggregateQuery.getEndTimestamp()))
                .stream().sorted(Comparator.comparingLong(EntityHistoryPO::getTimestamp)).toList();
        if (entityHistoryPOList.isEmpty()) {
            return entityAggregateResponse;
        }

        AggregateType aggregateType = entityAggregateQuery.getAggregateType();
        EntityHistoryPO oneEntityHistoryPO = entityHistoryPOList.get(0);
        switch (aggregateType) {
            case LAST:
                EntityHistoryPO lastEntityHistoryPO = entityHistoryPOList.get(entityHistoryPOList.size() - 1);
                if (lastEntityHistoryPO.getValueBoolean() != null) {
                    entityAggregateResponse.setValue(lastEntityHistoryPO.getValueBoolean());
                    entityAggregateResponse.setValueType(EntityValueType.BOOLEAN);
                } else if (lastEntityHistoryPO.getValueLong() != null) {
                    entityAggregateResponse.setValue(lastEntityHistoryPO.getValueLong().toString());
                    entityAggregateResponse.setValueType(EntityValueType.LONG);
                } else if (lastEntityHistoryPO.getValueDouble() != null) {
                    entityAggregateResponse.setValue(lastEntityHistoryPO.getValueDouble());
                    entityAggregateResponse.setValueType(EntityValueType.DOUBLE);
                } else if (lastEntityHistoryPO.getValueString() != null) {
                    entityAggregateResponse.setValue(lastEntityHistoryPO.getValueString());
                    entityAggregateResponse.setValueType(EntityValueType.STRING);
                } else if (lastEntityHistoryPO.getValueBinary() != null) {
                    entityAggregateResponse.setValue(lastEntityHistoryPO.getValueBinary());
                    entityAggregateResponse.setValueType(EntityValueType.BINARY);
                }
                break;
            case MIN:
                if (oneEntityHistoryPO.getValueBoolean() != null) {
                    EntityHistoryPO minEntityHistoryPO = entityHistoryPOList.stream().min(Comparator.comparing(EntityHistoryPO::getValueBoolean)).get();
                    entityAggregateResponse.setValue(minEntityHistoryPO.getValueBoolean());
                    entityAggregateResponse.setValueType(EntityValueType.BOOLEAN);
                } else if (oneEntityHistoryPO.getValueLong() != null) {
                    EntityHistoryPO minEntityHistoryPO = entityHistoryPOList.stream().min(Comparator.comparing(EntityHistoryPO::getValueLong)).get();
                    entityAggregateResponse.setValue(minEntityHistoryPO.getValueLong().toString());
                    entityAggregateResponse.setValueType(EntityValueType.LONG);
                } else if (oneEntityHistoryPO.getValueDouble() != null) {
                    EntityHistoryPO minEntityHistoryPO = entityHistoryPOList.stream().min(Comparator.comparing(EntityHistoryPO::getValueDouble)).get();
                    entityAggregateResponse.setValue(minEntityHistoryPO.getValueDouble());
                    entityAggregateResponse.setValueType(EntityValueType.DOUBLE);
                } else if (oneEntityHistoryPO.getValueString() != null) {
                    EntityHistoryPO minEntityHistoryPO = entityHistoryPOList.stream().min(Comparator.comparing(EntityHistoryPO::getValueString)).get();
                    entityAggregateResponse.setValue(minEntityHistoryPO.getValueString());
                    entityAggregateResponse.setValueType(EntityValueType.STRING);
                } else if (oneEntityHistoryPO.getValueBinary() != null) {
                    EntityHistoryPO minEntityHistoryPO = entityHistoryPOList.stream().min(Comparator.comparing(EntityHistoryPO::getValueBinary, byteArrayComparator)).get();
                    entityAggregateResponse.setValue(minEntityHistoryPO.getValueBinary());
                    entityAggregateResponse.setValueType(EntityValueType.BINARY);
                }
                break;
            case MAX:
                if (oneEntityHistoryPO.getValueBoolean() != null) {
                    entityAggregateResponse.setValue(entityHistoryPOList.stream().max(Comparator.comparing(EntityHistoryPO::getValueBoolean)).get().getValueBoolean());
                    entityAggregateResponse.setValueType(EntityValueType.BOOLEAN);
                } else if (oneEntityHistoryPO.getValueLong() != null) {
                    entityAggregateResponse.setValue(entityHistoryPOList.stream().max(Comparator.comparing(EntityHistoryPO::getValueLong)).get().getValueLong().toString());
                    entityAggregateResponse.setValueType(EntityValueType.LONG);
                } else if (oneEntityHistoryPO.getValueDouble() != null) {
                    entityAggregateResponse.setValue(entityHistoryPOList.stream().max(Comparator.comparing(EntityHistoryPO::getValueDouble)).get().getValueDouble());
                    entityAggregateResponse.setValueType(EntityValueType.DOUBLE);
                } else if (oneEntityHistoryPO.getValueString() != null) {
                    entityAggregateResponse.setValue(entityHistoryPOList.stream().max(Comparator.comparing(EntityHistoryPO::getValueString)).get().getValueString());
                    entityAggregateResponse.setValueType(EntityValueType.STRING);
                } else if (oneEntityHistoryPO.getValueBinary() != null) {
                    entityAggregateResponse.setValue(entityHistoryPOList.stream().max(Comparator.comparing(EntityHistoryPO::getValueBinary, byteArrayComparator)).get().getValueBinary());
                    entityAggregateResponse.setValueType(EntityValueType.BINARY);
                }
                break;
            case AVG:
                if (oneEntityHistoryPO.getValueBoolean() != null) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                } else if (oneEntityHistoryPO.getValueLong() != null) {
                    OptionalDouble average = entityHistoryPOList.stream()
                            .mapToLong(EntityHistoryPO::getValueLong)
                            .average();
                    if (average.isPresent()) {
                        String averageAsString = String.valueOf(average.getAsDouble());
                        entityAggregateResponse.setValue(averageAsString);
                    } else {
                        entityAggregateResponse.setValue("0");
                    }
                    entityAggregateResponse.setValueType(EntityValueType.LONG);
                } else if (oneEntityHistoryPO.getValueDouble() != null) {
                    BigDecimal sum = entityHistoryPOList.stream()
                            .filter(Objects::nonNull)
                            .filter(t -> t.getValueDouble() != null)
                            .map(t -> BigDecimal.valueOf(t.getValueDouble()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal count = new BigDecimal(entityHistoryPOList.size());
                    BigDecimal avg = sum.divide(count, 8, RoundingMode.HALF_EVEN);
                    entityAggregateResponse.setValue(avg.doubleValue());
                    entityAggregateResponse.setValueType(EntityValueType.DOUBLE);
                } else if (oneEntityHistoryPO.getValueString() != null) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                } else if (oneEntityHistoryPO.getValueBinary() != null) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                }
                break;
            case SUM:
                if (oneEntityHistoryPO.getValueBoolean() != null) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                } else if (oneEntityHistoryPO.getValueLong() != null) {
                    long sum = entityHistoryPOList.stream()
                            .mapToLong(EntityHistoryPO::getValueLong)
                            .sum();
                    String sumAsString = String.valueOf(sum);
                    entityAggregateResponse.setValue(sumAsString);
                    entityAggregateResponse.setValueType(EntityValueType.LONG);
                } else if (oneEntityHistoryPO.getValueDouble() != null) {
                    entityAggregateResponse.setValue(entityHistoryPOList.stream()
                            .filter(Objects::nonNull)
                            .filter(t -> t.getValueDouble() != null)
                            .map(t -> BigDecimal.valueOf(t.getValueDouble()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .doubleValue());
                    entityAggregateResponse.setValueType(EntityValueType.DOUBLE);
                } else if (oneEntityHistoryPO.getValueString() != null) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                } else if (oneEntityHistoryPO.getValueBinary() != null) {
                    throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                }
                break;
            case COUNT:
                break;
            default:
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
        if (aggregateType == AggregateType.COUNT) {
            List<EntityAggregateResponse.CountResult> countResult = new ArrayList<>();
            if (oneEntityHistoryPO.getValueBoolean() != null) {
                Map<Boolean, Integer> entityHistoryPOGroup = entityHistoryPOList.stream().collect(Collectors.groupingBy(EntityHistoryPO::getValueBoolean, Collectors.collectingAndThen(
                        Collectors.counting(),
                        Long::intValue
                )));
                entityHistoryPOGroup.forEach((key, value) -> countResult.add(new EntityAggregateResponse.CountResult(key, EntityValueType.BOOLEAN, value)));
            } else if (oneEntityHistoryPO.getValueLong() != null) {
                Map<String, Integer> entityHistoryPOGroup = entityHistoryPOList.stream().collect(Collectors.groupingBy(t -> t.getValueLong().toString(), Collectors.collectingAndThen(
                        Collectors.counting(),
                        Long::intValue
                )));
                entityHistoryPOGroup.forEach((key, value) -> countResult.add(new EntityAggregateResponse.CountResult(key, EntityValueType.LONG, value)));
            } else if (oneEntityHistoryPO.getValueDouble() != null) {
                Map<Double, Integer> entityHistoryPOGroup = entityHistoryPOList.stream().collect(Collectors.groupingBy(EntityHistoryPO::getValueDouble, Collectors.collectingAndThen(
                        Collectors.counting(),
                        Long::intValue
                )));
                entityHistoryPOGroup.forEach((key, value) -> countResult.add(new EntityAggregateResponse.CountResult(key, EntityValueType.DOUBLE, value)));
            } else if (oneEntityHistoryPO.getValueString() != null) {
                Map<String, Integer> entityHistoryPOGroup = entityHistoryPOList.stream().collect(Collectors.groupingBy(EntityHistoryPO::getValueString, Collectors.collectingAndThen(
                        Collectors.counting(),
                        Long::intValue
                )));
                entityHistoryPOGroup.forEach((key, value) -> countResult.add(new EntityAggregateResponse.CountResult(key, EntityValueType.STRING, value)));
            } else if (oneEntityHistoryPO.getValueBinary() != null) {
                Map<byte[], Integer> entityHistoryPOGroup = entityHistoryPOList.stream().collect(Collectors.groupingBy(EntityHistoryPO::getValueBinary, Collectors.collectingAndThen(
                        Collectors.counting(),
                        Long::intValue
                )));
                entityHistoryPOGroup.forEach((key, value) -> countResult.add(new EntityAggregateResponse.CountResult(key, EntityValueType.BINARY, value)));
            }
            entityAggregateResponse.setCountResult(countResult);
        }
        return entityAggregateResponse;
    }

    public EntityLatestResponse getEntityStatus(Long entityId) {
        EntityLatestResponse entityLatestResponse = new EntityLatestResponse();
        EntityPO entityPO = entityRepository.findOneWithDataPermission(filter -> filter.eq(EntityPO.Fields.id, entityId)).orElse(null);
        if (entityPO == null) {
            return entityLatestResponse;
        }

        EntityLatestValueCache lv = self().findSpecificEntityValue(List.of(entityPO)).get(0);
        if (lv.getValue() == null) {
            return entityLatestResponse;
        }

        entityLatestResponse.setUpdatedAt(lv.getUpdatedAt() == null ? null : lv.getUpdatedAt().toString());
        entityLatestResponse.setValueType(lv.getValueType());
        entityLatestResponse.setValue(lv.getValue());
        return entityLatestResponse;
    }

    private EntityValueService self() {
        return (EntityValueService) AopContext.currentProxy();
    }

}
