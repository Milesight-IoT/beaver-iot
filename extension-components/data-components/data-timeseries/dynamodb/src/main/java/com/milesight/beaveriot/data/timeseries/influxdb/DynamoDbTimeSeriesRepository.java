package com.milesight.beaveriot.data.timeseries.influxdb;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.model.TimeSeriesCursor;
import com.milesight.beaveriot.data.model.TimeSeriesPeriodQuery;
import com.milesight.beaveriot.data.model.TimeSeriesResult;
import com.milesight.beaveriot.data.model.TimeSeriesTimePointQuery;
import com.milesight.beaveriot.data.support.TimeSeriesDataConverter;
import com.milesight.beaveriot.data.timeseries.common.TimeSeriesProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/11/3 8:56
 **/
@Slf4j
public class DynamoDbTimeSeriesRepository<T> implements TimeSeriesRepository<T> {
    @Resource
    private DynamoDbClient client;
    @Resource
    private TimeSeriesProperty property;
    private final String category;
    private final String tableName;
    private final String timeColumn;
    private final Set<String> indexedColumns;
    private final TimeSeriesDataConverter converter;
    private final Class<T> poClass;
    private final Map<String, Field> poFields;
    private String partitionKey;
    private String sortKey;
    private ScalarAttributeType partitionKeyScalarAttributeType;
    private String expireTimeKey;
    private Duration expireDuration;

    public DynamoDbTimeSeriesRepository(String category, String tableName, String timeColumn, Set<String> indexedColumns, TimeSeriesDataConverter converter, Class<T> poClass) {
        this.category = category;
        this.tableName = tableName;
        this.timeColumn = timeColumn;
        this.indexedColumns = indexedColumns;
        this.converter = converter;
        this.poClass = poClass;
        this.poFields = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        this.partitionKey = getPartitionKey();
        this.sortKey = getSortKey();
        this.partitionKeyScalarAttributeType = getPartitionKeyScalarAttributeType(partitionKey);
        this.expireTimeKey = getExpireTimeKey();
        this.expireDuration = property.getRetention().get(category);
        ensureTable();
    }

    public void ensureTable() {
        try {
            client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
        } catch (ResourceNotFoundException e) {
            createTable();
            enableTTL();
        }
    }

    private void createTable() {
        CreateTableRequest.Builder requestBuilder = CreateTableRequest.builder()
                .tableName(tableName)
                .billingMode(BillingMode.PAY_PER_REQUEST);

        requestBuilder.keySchema(
                KeySchemaElement.builder()
                        .attributeName(partitionKey)
                        .keyType(KeyType.HASH)
                        .build(),
                KeySchemaElement.builder()
                        .attributeName(sortKey)
                        .keyType(KeyType.RANGE)
                        .build()
        );

        requestBuilder.attributeDefinitions(
                AttributeDefinition.builder()
                        .attributeName(partitionKey)
                        .attributeType(getPartitionKeyScalarAttributeType(partitionKey))
                        .build(),
                AttributeDefinition.builder()
                        .attributeName(sortKey)
                        .attributeType(ScalarAttributeType.N)
                        .build()
        );

        client.createTable(requestBuilder.build());
        log.debug("Created table '{}'", tableName);
    }

    private void enableTTL() {
        try {
            DescribeTimeToLiveResponse response = client.describeTimeToLive(
                    DescribeTimeToLiveRequest.builder()
                            .tableName(tableName)
                            .build()
            );

            TimeToLiveDescription status = response.timeToLiveDescription();
            if (status == null || status.timeToLiveStatus() != TimeToLiveStatus.ENABLED) {
                client.updateTimeToLive(
                        UpdateTimeToLiveRequest.builder()
                                .tableName(tableName)
                                .timeToLiveSpecification(TimeToLiveSpecification.builder()
                                        .attributeName(getExpireTimeKey())
                                        .enabled(true)
                                        .build())
                                .build()
                );
                log.debug("Enabled TTL on table '{}' with attribute '{}'", tableName, getExpireTimeKey());
            }
        } catch (Exception e) {
            log.error("Failed to enable TTL on table: " + tableName, e);
        }
    }

    private String getPartitionKey() {
        if (CollectionUtils.isEmpty(indexedColumns)) {
            throw new IllegalArgumentException("No indexed columns defined");
        }

        return indexedColumns.size() == 1 ? indexedColumns.stream().findFirst().get() : DynamoDbConstants.PARTITION_KEY;
    }

    private String getSortKey() {
        return timeColumn;
    }

    private String getExpireTimeKey() {
        return DynamoDbConstants.EXPIRE_TIME_KEY;
    }

    @Override
    public TimeSeriesResult<T> findByTimePoints(TimeSeriesTimePointQuery query) {
        if (query.getTimestampList() == null || query.getTimestampList().isEmpty()) {
            return TimeSeriesResult.of();
        }

        query.validate(indexedColumns);

        List<T> poList = new ArrayList<>();
        query.getTimestampList().forEach(timestamp -> {
            QueryResponse queryResponse = client.query(new DynamoDbQueryBuilder(tableName)
                    .indexedColumns(indexedColumns)
                    .indexedKeyValues(query.getIndexedKeyValues())
                    .timeColumn(timeColumn)
                    .time(timestamp)
                    .filter(query.getFilterable())
                    .build());
            List<T> subList = convertToPOList(queryResponse.items());
            if (!CollectionUtils.isEmpty(subList)) {
                poList.addAll(subList);
            }
        });
        return TimeSeriesResult.of(poList);
    }

    @Override
    public TimeSeriesResult<T> findByPeriod(TimeSeriesPeriodQuery query) {
        query.validate(indexedColumns);

        QueryResponse queryResponse = client.query(new DynamoDbQueryBuilder(tableName)
                .indexedColumns(indexedColumns)
                .indexedKeyValues(query.getIndexedKeyValues())
                .timeColumn(timeColumn)
                .start(query.getStartTimestamp())
                .end(query.getEndTimestamp())
                .filter(query.getFilterable())
                .order(query.getOrder())
                .limit(Math.toIntExact(query.getPageSize()))
                .cursor(query.getCursor())
                .build());

        List<T> poList = convertToPOList(queryResponse.items());
        TimeSeriesCursor cursor = null;

        if (queryResponse.hasLastEvaluatedKey()) {
            Map<String, AttributeValue> lastEvaluatedKey = queryResponse.lastEvaluatedKey();
            Map<String, Object> indexedKeyValues = toIndexedKeyValues(lastEvaluatedKey);
            Long timestamp = (Long) indexedKeyValues.get(timeColumn);
            cursor = TimeSeriesCursor.of(timestamp, indexedKeyValues);
        }
        return TimeSeriesResult.of(poList, cursor);
    }

    @Override
    public void save(List<T> itemList) {
        if (CollectionUtils.isEmpty(itemList)) {
            return;
        }

        final int batchSize = DynamoDbConstants.SAVE_BATCH_SIZE;
        for (int i = 0; i < itemList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, itemList.size());
            List<T> subList = itemList.subList(i, end);

            Map<String, List<WriteRequest>> requestItems = Map.of(
                    tableName,
                    subList.stream().map(po -> {
                        Map<String, AttributeValue> itemMap = new HashMap<>();

                        Map<String, Object> poMap = this.converter.toMap(po);

                        String partitionValue = getPartitionValue(poMap);
                        AttributeValue partitionKeyAttributeValue = getPartitionKeyAttributeValue(partitionValue);
                        itemMap.put(partitionKey, partitionKeyAttributeValue);
                        Long timestamp = (Long) poMap.get(timeColumn);
                        itemMap.put(sortKey, AttributeValue.builder().n(Long.toString(timestamp)).build());

                        poMap.forEach((key, value) -> {
                            if (sortKey.equals(key)) {
                                return;
                            }

                            AttributeValue attributeValue = DynamoDbSupport.toAttributeValue(value);
                            if (attributeValue == null) {
                                return;
                            }

                            itemMap.put(key, attributeValue);
                        });

                        long expireTime = (timestamp + expireDuration.toMillis()) / 1000;
                        itemMap.put(expireTimeKey, AttributeValue.builder().n(Long.toString(expireTime)).build());

                        return WriteRequest.builder()
                                .putRequest(PutRequest.builder().item(itemMap).build())
                                .build();
                    }).toList()
            );

            client.batchWriteItem(request -> request.requestItems(requestItems));
        }
    }

    private Map<String, Object> toIndexedKeyValues(Map<String, AttributeValue> lastEvaluatedKey) {
        return lastEvaluatedKey.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> DynamoDbSupport.toValue(entry.getValue())));
    }

    private List<T> convertToPOList(List<Map<String, AttributeValue>> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }

        List<T> poList = new ArrayList<>();
        Set<String> fieldNames = getPoFields().keySet();
        for (Map<String, AttributeValue> item : items) {
            Map<String, Object> poMap = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                String itemKey = entry.getKey();
                AttributeValue itemValue = entry.getValue();
                if (!fieldNames.contains(itemKey)) {
                    continue;
                }

                Optional<Object> optionalValue = itemValue.getValueForField(itemValue.type().toString(), Object.class);
                optionalValue.ifPresent(object -> poMap.put(itemKey, object));
            }
            poList.add(converter.fromMap(poMap, poClass));
        }
        return poList;
    }

    private String getPartitionValue(Map<String, Object> poMap) {
        return indexedColumns.stream()
                .sorted()
                .map(key -> poMap.get(key).toString())
                .collect(Collectors.joining(DynamoDbConstants.PARTITION_VALUE_SEPARATOR));
    }

    private AttributeValue getPartitionKeyAttributeValue(String partitionValue) {
        return switch (partitionKeyScalarAttributeType) {
            case S -> AttributeValue.builder().s(partitionValue).build();
            case N -> AttributeValue.builder().n(partitionValue).build();
            default ->
                    throw new IllegalArgumentException("Invalid partition key type: " + partitionKeyScalarAttributeType);
        };
    }

    private ScalarAttributeType getPartitionKeyScalarAttributeType(String partitionKey) {
        if (DynamoDbConstants.PARTITION_KEY.equals(partitionKey)) {
            return ScalarAttributeType.S;
        } else {
            Map<String, Field> poFields = getPoFields();
            Field field = poFields.get(partitionKey);
            if (field == null) {
                throw new IllegalArgumentException("Invalid partition key: " + partitionKey);
            }

            Class<?> fieldType = field.getType();
            if (fieldType == String.class) {
                return ScalarAttributeType.S;
            } else if (fieldType == Long.class || fieldType == Integer.class) {
                return ScalarAttributeType.N;
            } else if (fieldType == Double.class || fieldType == Float.class) {
                return ScalarAttributeType.N;
            } else {
                throw new IllegalArgumentException("Invalid partition key type: " + fieldType.getName());
            }
        }
    }

    private Map<String, Field> getPoFields() {
        if (CollectionUtils.isEmpty(poFields)) {
            Field[] fields = poClass.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = StringUtils.toSnakeCase(field.getName());
                poFields.put(fieldName, field);
            }
        }
        return poFields;
    }
}