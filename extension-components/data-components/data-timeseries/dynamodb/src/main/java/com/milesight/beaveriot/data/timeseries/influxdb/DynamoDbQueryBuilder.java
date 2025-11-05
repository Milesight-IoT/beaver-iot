package com.milesight.beaveriot.data.timeseries.influxdb;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.filterable.SearchFilter;
import com.milesight.beaveriot.data.filterable.condition.CompareCondition;
import com.milesight.beaveriot.data.filterable.condition.CompositeCondition;
import com.milesight.beaveriot.data.filterable.condition.Condition;
import com.milesight.beaveriot.data.filterable.enums.BooleanOperator;
import com.milesight.beaveriot.data.filterable.enums.SearchOperator;
import com.milesight.beaveriot.data.model.TimeSeriesCursor;
import com.milesight.beaveriot.data.model.TimeSeriesQueryOrder;
import org.springframework.data.util.Pair;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/11/3 16:14
 **/
public class DynamoDbQueryBuilder {
    private final String tableName;
    private String timeColumn;
    private Set<String> indexedColumns;
    private Map<String, Object> indexedKeyValues;
    private Long start;
    private Long end;
    private Long time;
    private TimeSeriesQueryOrder order = TimeSeriesQueryOrder.DESC;
    private TimeSeriesCursor cursor;
    private Consumer<Filterable> filter;
    private Integer limit;

    public DynamoDbQueryBuilder(String tableName) {
        this.tableName = tableName;
    }

    public DynamoDbQueryBuilder timeColumn(String timeColumn) {
        this.timeColumn = timeColumn;
        return this;
    }

    public DynamoDbQueryBuilder indexedColumns(Set<String> indexedColumns) {
        this.indexedColumns = indexedColumns;
        return this;
    }

    public DynamoDbQueryBuilder indexedKeyValues(Map<String, Object> indexedKeyValues) {
        this.indexedKeyValues = indexedKeyValues;
        return this;
    }

    private void validateTime() {
        if (this.start != null && this.end != null) {
            if (this.end < this.start) {
                throw new IllegalArgumentException("End must be larger than start.");
            }
            return;
        }

        if (this.time != null) {
            return;
        }

        throw new IllegalArgumentException("Either Start and end or time must be specified.");
    }

    public DynamoDbQueryBuilder start(long startRange) {
        if (startRange <= 0) {
            throw new IllegalArgumentException("start of range must be larger than zero");
        }

        this.start = startRange;
        return this;
    }

    public DynamoDbQueryBuilder end(long endRange) {
        if (endRange <= 0) {
            throw new IllegalArgumentException("end of range must be larger than zero");
        }

        this.end = endRange;
        return this;
    }

    public DynamoDbQueryBuilder time(long time) {
        if (time <= 0) {
            throw new IllegalArgumentException("time must be larger than zero");
        }

        this.time = time;
        return this;
    }

    public DynamoDbQueryBuilder order(TimeSeriesQueryOrder order) {
        this.order = order;
        return this;
    }

    public DynamoDbQueryBuilder filter(Consumer<Filterable> filter) {
        this.filter = filter;
        return this;
    }

    public DynamoDbQueryBuilder cursor(TimeSeriesCursor cursor) {
        this.cursor = cursor;
        return this;
    }

    public DynamoDbQueryBuilder limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public QueryRequest build() {
        this.validateTime();

        QueryRequest.Builder builder = QueryRequest.builder()
                .tableName(tableName);

        Map<String, String> expressionAttributeNames = new HashMap<>();
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        String keyConditionExpression = buildKeyConditionExpression(expressionAttributeNames, expressionAttributeValues);
        builder.keyConditionExpression(keyConditionExpression);

        if (filter != null) {
            String filterExpression = buildFilterExpression(filter, expressionAttributeNames, expressionAttributeValues);
            builder.filterExpression(filterExpression);
        }

        builder.expressionAttributeNames(expressionAttributeNames);
        builder.expressionAttributeValues(expressionAttributeValues);

        builder.scanIndexForward(Objects.equals(order, TimeSeriesQueryOrder.ASC));

        if (cursor != null) {
            builder.exclusiveStartKey(getExclusiveStartKey(cursor.getIndexedKeyValues()));
        }

        if (limit != null) {
            builder.limit(limit);
        }

        return builder.build();
    }

    private Map<String, AttributeValue> getExclusiveStartKey(Map<String, Object> indexedKeyValues) {
        return indexedKeyValues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> DynamoDbSupport.toAttributeValue(entry.getValue())
                ));
    }
    private String buildKeyConditionExpression(Map<String, String> expressionAttributeNames, Map<String, AttributeValue> expressionAttributeValues) {
        Consumer<Filterable> filterable = toIndexFilterable(indexedKeyValues);

        if (time != null) {
            filterable = filterable.andThen(f -> f.eq(timeColumn, time));
        } else if (start != null && end != null) {
            filterable = filterable.andThen(f -> f.between(timeColumn, start, end));
        }

        SearchFilter queryFilter = new SearchFilter(BooleanOperator.AND, new ArrayList<>());
        filterable.accept(queryFilter);
        return buildFilterExpression(queryFilter, expressionAttributeNames, expressionAttributeValues);
    }

    private String buildFilterExpression(Consumer<Filterable> filterable,
                                                Map<String, String> expressionAttributeNames,
                                                Map<String, AttributeValue> expressionAttributeValues) {
        SearchFilter queryFilter = new SearchFilter(BooleanOperator.AND, new ArrayList<>());
        filterable.accept(queryFilter);
        return buildFilterExpression(queryFilter, expressionAttributeNames, expressionAttributeValues);
    }

    private static String buildFilterExpression(CompositeCondition condition,
                                                Map<String, String> expressionAttributeNames,
                                                Map<String, AttributeValue> expressionAttributeValues) {
        List<String> parts = new ArrayList<>();
        for (Condition cond : condition.getConditions()) {
            if (cond instanceof CompareCondition compareCondition) {
                parts.add(buildCompareExpression(compareCondition, expressionAttributeNames, expressionAttributeValues));
            } else if (cond instanceof CompositeCondition compositeCondition) {
                String sub = buildFilterExpression(compositeCondition, expressionAttributeNames, expressionAttributeValues);
                if (!sub.isEmpty()) {
                    parts.add("(" + sub + ")");
                }
            }
        }
        String op = condition.getBooleanOperator() == BooleanOperator.AND ? " AND " : " OR ";
        return String.join(op, parts);
    }

    private static String buildCompareExpression(CompareCondition cond,
                                                 Map<String, String> expressionAttributeNames,
                                                 Map<String, AttributeValue> expressionAttributeValues) {
        String name = StringUtils.toSnakeCase(cond.getName());
        Object value = cond.getValue();
        SearchOperator op = cond.getSearchOperator();

        String placeholderName = placeholderName(name);
        expressionAttributeNames.put(placeholderName, name);
        String opSuffix = op.name().toLowerCase();
        Map<String, AtomicLong> placeholderValueCounter = new HashMap<>();

        return switch (op) {
            case EQ -> placeholderName + " = " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case NE -> placeholderName + " != " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case GT -> placeholderName + " > " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case GE -> placeholderName + " >= " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case LT -> placeholderName + " < " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case LE -> placeholderName + " <= " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case IN -> placeholderName + " IN " + placeholderValueIn(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case NOT_IN -> placeholderName + " NOT IN " + placeholderValueIn(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter);
            case IS_NULL -> placeholderName + " IS NULL";
            case IS_NOT_NULL -> placeholderName + " IS NOT NULL";
            case LIKE -> "contains(" + placeholderName + ", " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter) + ")";
            case NOT_LIKE -> "NOT contains(" + placeholderName + ", " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter) + ")";
            case STARTS_WITH -> "begins_with(" + placeholderName + ", " + placeholderValue(name, opSuffix, value, expressionAttributeValues, placeholderValueCounter) + ")";
            case BETWEEN -> {
                Pair<?, ?> pair = (Pair<?, ?>) value;
                Object firstValue = pair.getFirst();
                Object secondValue = pair.getSecond();
                yield placeholderName + " BETWEEN " + placeholderValue(name, opSuffix + "_start", firstValue, expressionAttributeValues, placeholderValueCounter) + " AND " + placeholderValue(name, opSuffix + "_end", secondValue, expressionAttributeValues, placeholderValueCounter);
            }
            // Not supported: CASE_IGNORE_LIKE, CASE_IGNORE_NOT_LIKE, CASE_IGNORE_STARTS_WITH, ENDS_WITH, CASE_IGNORE_ENDS_WITH
            default -> throw new UnsupportedOperationException("Unsupported operator: " + op);
        };
    }

    private static String placeholderValueIn(String name,
                                             String opSuffix,
                                             Object value,
                                             Map<String, AttributeValue> expressionAttributeValues,
                                             Map<String, AtomicLong> placeholderValueCounter) {
        Object[] arrayValues = (Object[]) value;
        List<String> placeholderValues = new ArrayList<>();
        for (int i = 0; i < arrayValues.length; i++) {
            Object arrayValue = arrayValues[i];
            String placeholderValue = placeholderValue(name, opSuffix + "_" + (i + 1), arrayValue, expressionAttributeValues, placeholderValueCounter);
            placeholderValues.add(placeholderValue);
        }
        return "(" + String.join(", ", placeholderValues) + ")";
    }

    private static String placeholderValue(String name,
                                           String suffix,
                                           Object value, Map<String, AttributeValue> expressionAttributeValues,
                                           Map<String, AtomicLong> placeholderValueCounter) {
        String placeholderValue = DynamoDbConstants.PLACEHOLDER_VALUE + name + "_" + suffix;
        AttributeValue newValue = DynamoDbSupport.toAttributeValue(value);
        if (expressionAttributeValues.containsKey(placeholderValue)) {
            AttributeValue existingValue = expressionAttributeValues.get(placeholderValue);
            if (!existingValue.equals(newValue)) {
                placeholderValue = placeholderValue + "_" + placeholderValueCounter.computeIfAbsent(placeholderValue, k -> new AtomicLong()).incrementAndGet();
            }
        }
        expressionAttributeValues.put(placeholderValue, newValue);
        return placeholderValue;
    }

    private static String placeholderName(String name) {
        return DynamoDbConstants.PLACEHOLDER_NAME + name;
    }

    private Consumer<Filterable> toIndexFilterable(Map<String, Object> indexedKeyValues) {
        Map<String, Object> snakeCaseIndexedKeyValues = indexedKeyValues.entrySet().stream()
                .collect(Collectors.toMap(entry -> StringUtils.toSnakeCase(entry.getKey()), Map.Entry::getValue));
        if (indexedColumns.size() == 1) {
            String indexedColumn = indexedColumns.stream().findFirst().get();
            return f -> f.eq(indexedColumn, snakeCaseIndexedKeyValues.get(indexedColumn));
        } else {
            return f -> f.eq(DynamoDbConstants.PARTITION_KEY, indexedColumns.stream()
                    .sorted()
                    .map(key -> snakeCaseIndexedKeyValues.get(key).toString())
                    .collect(Collectors.joining(DynamoDbConstants.PARTITION_VALUE_SEPARATOR)));
        }
    }
}
