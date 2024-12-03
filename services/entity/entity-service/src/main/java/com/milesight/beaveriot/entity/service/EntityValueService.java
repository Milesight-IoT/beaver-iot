package com.milesight.beaveriot.entity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
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
import jakarta.persistence.EntityManager;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
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
    public void saveValues(Map<String, Object> values) {
        saveValues(values, System.currentTimeMillis());
    }

    @Override
    public void saveValues(Map<String, Object> values, long timestamp) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> entityKeys = values.keySet().stream().toList();
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return;
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
            if (payload != null) {
                switch (entityValueType) {
                    case OBJECT:
                        // do nothing
                        break;
                    case BOOLEAN:
                        entityLatestPO.setValueBoolean(JsonUtils.cast(payload, Boolean.class));
                        break;
                    case LONG:
                        entityLatestPO.setValueLong(JsonUtils.cast(payload, Long.class));
                        break;
                    case STRING:
                        entityLatestPO.setValueString(JsonUtils.cast(payload, String.class));
                        break;
                    case DOUBLE:
                        entityLatestPO.setValueDouble(JsonUtils.cast(payload, BigDecimal.class));
                        break;
                    case BINARY:
                        if (payload instanceof byte[] bytes) {
                            entityLatestPO.setValueBinary(bytes);
                        } else {
                            entityLatestPO.setValueBinary(Base64.getDecoder().decode(String.valueOf(payload)));
                        }
                        break;
                    default:
                        throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                }
            }
            entityLatestPO.setTimestamp(timestamp);
            entityLatestPOList.add(entityLatestPO);
        });
        entityLatestRepository.saveAll(entityLatestPOList);
    }

    @Override
    public void saveHistoryRecord(Map<String, Object> recordValues) {
        saveHistoryRecord(recordValues, System.currentTimeMillis());
    }

    @Override
    public void saveHistoryRecord(Map<String, Object> recordValues, long timestamp) {
        if (recordValues == null || recordValues.isEmpty()) {
            return;
        }
        List<String> entityKeys = recordValues.keySet().stream().toList();
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(EntityPO.Fields.key, entityKeys.toArray()));
        if (entityPOList == null || entityPOList.isEmpty()) {
            return;
        }
        Map<String, EntityPO> entityKeyMap = entityPOList.stream().collect(Collectors.toMap(EntityPO::getKey, Function.identity()));
        List<EntityHistoryUnionQuery> entityHistoryUnionQueryList = recordValues.keySet().stream().map(o -> {
            EntityPO entityPO = entityKeyMap.get(o);
            if (entityPO == null) {
                return null;
            }
            Long entityId = entityPO.getId();
            EntityHistoryUnionQuery entityHistoryUnionQuery = new EntityHistoryUnionQuery();
            entityHistoryUnionQuery.setEntityId(entityId);
            entityHistoryUnionQuery.setTimestamp(timestamp);
            return entityHistoryUnionQuery;
        }).filter(Objects::nonNull).toList();
        List<EntityHistoryPO> existEntityHistoryPOList = entityHistoryRepository.findByUnionUnique(entityManager, entityHistoryUnionQueryList);
        Map<String, EntityHistoryPO> existUnionIdMap = new HashMap<>();
        if (existEntityHistoryPOList != null && !existEntityHistoryPOList.isEmpty()) {
            existUnionIdMap.putAll(existEntityHistoryPOList.stream().collect(Collectors.toMap(entityHistoryPO -> entityHistoryPO.getEntityId() + ":" + entityHistoryPO.getTimestamp(), Function.identity())));
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
            String unionId = entityId + ":" + timestamp;
            EntityHistoryPO dataHistory = existUnionIdMap.get(unionId);
            Long historyId = null;
            String operatorId = SecurityUserContext.getUserId();
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
            if (payload != null) {
                switch (entityValueType) {
                    case OBJECT:
                        // do nothing
                        break;
                    case BOOLEAN:
                        entityHistoryPO.setValueBoolean(JsonUtils.cast(payload, Boolean.class));
                        break;
                    case LONG:
                        entityHistoryPO.setValueLong(JsonUtils.cast(payload, Long.class));
                        break;
                    case STRING:
                        entityHistoryPO.setValueString(JsonUtils.cast(payload, String.class));
                        break;
                    case DOUBLE:
                        entityHistoryPO.setValueDouble(JsonUtils.cast(payload, BigDecimal.class));
                        break;
                    case BINARY:
                        if (payload instanceof byte[] bytes) {
                            entityHistoryPO.setValueBinary(bytes);
                        } else {
                            entityHistoryPO.setValueBinary(Base64.getDecoder().decode(String.valueOf(payload)));
                        }
                        break;
                    default:
                        throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
                }
            }
            entityHistoryPO.setTimestamp(timestamp);
            entityHistoryPO.setUpdatedBy(operatorId);
            entityHistoryPOList.add(entityHistoryPO);
        });
        entityHistoryRepository.saveAll(entityHistoryPOList);
    }

    @Override
    public JsonNode findValueByKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return findValuesByKeys(Collections.singletonList(key)).get(key);
    }

    @Override
    @NonNull
    public Map<String, JsonNode> findValuesByKeys(List<String> keys) {
        Map<String, JsonNode> resultMap = new HashMap<>();
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
        List<Long> entityIds = allEntities.stream().map(EntityPO::getId).toList();
        Map<Long, String> entityKeyMap = allEntities.stream().collect(Collectors.toMap(EntityPO::getId, EntityPO::getKey));
        List<EntityLatestPO> entityLatestPOList = entityLatestRepository.findAll(filter -> filter.in(EntityLatestPO.Fields.entityId, entityIds.toArray()));
        if (entityLatestPOList == null || entityLatestPOList.isEmpty()) {
            return resultMap;
        }
        entityLatestPOList.forEach(entityLatestPO -> {
            JsonNode value = null;
            JsonNodeFactory nodeFactory = JsonUtils.getObjectMapper().getNodeFactory();
            if (entityLatestPO.getValueBoolean() != null) {
                value = nodeFactory.booleanNode(entityLatestPO.getValueBoolean());
            } else if (entityLatestPO.getValueLong() != null) {
                value = nodeFactory.numberNode(entityLatestPO.getValueLong());
            } else if (entityLatestPO.getValueDouble() != null) {
                value = nodeFactory.numberNode(entityLatestPO.getValueDouble());
            } else if (entityLatestPO.getValueString() != null) {
                value = nodeFactory.textNode(entityLatestPO.getValueString());
            } else if (entityLatestPO.getValueBinary() != null) {
                value = nodeFactory.binaryNode(entityLatestPO.getValueBinary());
            }
            resultMap.put(entityKeyMap.get(entityLatestPO.getEntityId()), value);
        });
        return resultMap;
    }

    @Override
    @NonNull
    public <T extends ExchangePayload> T findValuesByKey(String key, Class<T> entitiesClazz) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("key is empty");
        }
        Map<String, JsonNode> exchangeValues = findValuesByKeys(Collections.singletonList(key));
        return new MapExchangePayloadProxy<>(exchangeValues, entitiesClazz).proxy();
    }

    public Page<EntityHistoryResponse> historySearch(EntityHistoryQuery entityHistoryQuery) {
        Page<EntityHistoryPO> entityHistoryPage = entityHistoryRepository.findAll(f -> f.eq(EntityHistoryPO.Fields.entityId, entityHistoryQuery.getEntityId())
                        .ge(EntityHistoryPO.Fields.timestamp, entityHistoryQuery.getStartTimestamp())
                        .le(EntityHistoryPO.Fields.timestamp, entityHistoryQuery.getEndTimestamp()),
                entityHistoryQuery.toPageable());
        if (entityHistoryPage == null || entityHistoryPage.getContent().isEmpty()) {
            return Page.empty();
        }
        return entityHistoryPage.map(entityHistoryPO -> {
            EntityHistoryResponse response = new EntityHistoryResponse();
            if (entityHistoryPO.getValueBoolean() != null) {
                response.setValue(entityHistoryPO.getValueBoolean());
                response.setValueType(EntityValueType.BOOLEAN);
            } else if (entityHistoryPO.getValueLong() != null) {
                response.setValue(entityHistoryPO.getValueLong().toString());
                response.setValueType(EntityValueType.LONG);
            } else if (entityHistoryPO.getValueDouble() != null) {
                response.setValue(entityHistoryPO.getValueDouble().doubleValue());
                response.setValueType(EntityValueType.DOUBLE);
            } else if (entityHistoryPO.getValueString() != null) {
                response.setValue(entityHistoryPO.getValueString());
                response.setValueType(EntityValueType.STRING);
            } else if (entityHistoryPO.getValueBinary() != null) {
                response.setValue(entityHistoryPO.getValueBinary());
                response.setValueType(EntityValueType.BINARY);
            }
            response.setTimestamp(entityHistoryPO.getTimestamp().toString());
            return response;
        });
    }

    public EntityAggregateResponse historyAggregate(EntityAggregateQuery entityAggregateQuery) {
        List<EntityHistoryPO> entityHistoryPOList = entityHistoryRepository.findAll(filter -> filter.eq(EntityHistoryPO.Fields.entityId, entityAggregateQuery.getEntityId())
                        .ge(EntityHistoryPO.Fields.timestamp, entityAggregateQuery.getStartTimestamp())
                        .le(EntityHistoryPO.Fields.timestamp, entityAggregateQuery.getEndTimestamp()))
                .stream().sorted(Comparator.comparingLong(EntityHistoryPO::getTimestamp)).toList();
        EntityAggregateResponse entityAggregateResponse = new EntityAggregateResponse();
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
                    entityAggregateResponse.setValue(lastEntityHistoryPO.getValueDouble().doubleValue());
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
                    entityAggregateResponse.setValue(minEntityHistoryPO.getValueDouble().doubleValue());
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
                    entityAggregateResponse.setValue(entityHistoryPOList.stream().max(Comparator.comparing(EntityHistoryPO::getValueDouble)).get().getValueDouble().doubleValue());
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
                    BigDecimal sum = entityHistoryPOList.stream().map(EntityHistoryPO::getValueDouble).reduce(BigDecimal.ZERO, BigDecimal::add);
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
                    entityAggregateResponse.setValue(entityHistoryPOList.stream().map(EntityHistoryPO::getValueDouble).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
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
                Map<Double, Integer> entityHistoryPOGroup = entityHistoryPOList.stream().collect(Collectors.groupingBy(t -> t.getValueDouble().doubleValue(), Collectors.collectingAndThen(
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
        EntityLatestPO entityLatestPO = entityLatestRepository.findOne(filter -> filter.eq(EntityLatestPO.Fields.entityId, entityId)).orElse(null);
        EntityLatestResponse entityLatestResponse = new EntityLatestResponse();
        if (entityLatestPO == null) {
            return entityLatestResponse;
        }
        if (entityLatestPO.getValueBoolean() != null) {
            entityLatestResponse.setValue(entityLatestPO.getValueBoolean());
            entityLatestResponse.setValueType(EntityValueType.BOOLEAN);
        } else if (entityLatestPO.getValueLong() != null) {
            entityLatestResponse.setValue(entityLatestPO.getValueLong().toString());
            entityLatestResponse.setValueType(EntityValueType.LONG);
        } else if (entityLatestPO.getValueDouble() != null) {
            entityLatestResponse.setValue(entityLatestPO.getValueDouble().doubleValue());
            entityLatestResponse.setValueType(EntityValueType.DOUBLE);
        } else if (entityLatestPO.getValueString() != null) {
            entityLatestResponse.setValue(entityLatestPO.getValueString());
            entityLatestResponse.setValueType(EntityValueType.STRING);
        } else if (entityLatestPO.getValueBinary() != null) {
            entityLatestResponse.setValue(entityLatestPO.getValueBinary());
            entityLatestResponse.setValueType(EntityValueType.BINARY);
        }
        entityLatestResponse.setUpdatedAt(entityLatestPO.getUpdatedAt() == null ? null : entityLatestPO.getUpdatedAt().toString());
        return entityLatestResponse;
    }

}
