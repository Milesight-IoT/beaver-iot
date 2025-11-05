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
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * FluxQueryBuilder class.
 *
 * @author simon
 * @date 2025/10/14
 */
public class FluxQueryBuilder {

    private static final String RECORD_TEMP_VARIABLE = "r";

    private static final String RECORD_TEMP_TIME_VARIABLE = formatColumnRef(InfluxDbConstants.TIME_COLUMN);

    private final String bucket;

    private final String measurement;

    private Integer limit;

    private Consumer<Filterable> filter;

    private Map<String, Object> indexedKeyValues;

    private Long start;

    private Long end;

    private TimeSeriesQueryOrder order = TimeSeriesQueryOrder.DESC;

    private Set<String> indexedColumns;

    private TimeSeriesCursor cursor;

    public FluxQueryBuilder filter(Consumer<Filterable> filter) {
        this.filter = filter;
        return this;
    }

    public FluxQueryBuilder limit(int queryLimit) {
        if (queryLimit <= 0) {
            throw new IllegalArgumentException("Query limit must be larger than zero");
        }

        this.limit = queryLimit;
        return this;
    }

    public FluxQueryBuilder indexedColumns(Set<String> indexedColumns) {
        this.indexedColumns = indexedColumns;
        return this;
    }

    public FluxQueryBuilder cursor(TimeSeriesCursor cursor) {
        this.cursor = cursor;
        return this;
    }

    public FluxQueryBuilder indexedKeyValues(Map<String, Object> indexedKeyValues) {
        this.indexedKeyValues = indexedKeyValues;
        return this;
    }

    private void validateStartAndEnd() {
        if (this.end == null || this.start == null) {
            throw new IllegalArgumentException("Start and end must be specified.");
        }

        if (this.end < this.start) {
            throw new IllegalArgumentException("End must be larger than start.");
        }
    }

    public FluxQueryBuilder start(long startRange) {
        if (startRange <= 0) {
            throw new IllegalArgumentException("start of range must be larger than zero");
        }

        this.start = startRange;
        return this;
    }

    public FluxQueryBuilder end(long endRange) {
        if (endRange <= 0) {
            throw new IllegalArgumentException("end of range must be larger than zero");
        }

        this.end = endRange;
        return this;
    }

    public FluxQueryBuilder order(TimeSeriesQueryOrder order) {
        this.order = order;
        return this;
    }

    public FluxQueryBuilder(String bucket, String measurement) {
        this.bucket = bucket;
        this.measurement = measurement;
    }

    public String build() {
        this.validateStartAndEnd();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("from(bucket: \"%s\")\n", bucket));
        sb.append(String.format("  |> range(start: %s, stop: %s)\n", Instant.ofEpochMilli(this.start).toString(), Instant.ofEpochMilli(this.end).toString()));
        sb.append(String.format("  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\"", measurement));

        Consumer<Filterable> filterable = toIndexFilterable(indexedKeyValues);
        if (filter != null) {
            filterable = filterable.andThen(filter);
        }

        SearchFilter queryFilter = new SearchFilter(BooleanOperator.AND, new ArrayList<>());
        filterable.accept(queryFilter);
        String filterExpression = buildFilterExpression(queryFilter);
        sb.append(" and ").append(filterExpression).append(")\n");

        if (cursor != null && !cursor.getIndexedKeyValues().isEmpty()) {
            sb.append("  |> filter(fn: (r) => ").append(getSortKeyFilter()).append(")\n");
        }

        sb.append(String.format("  |> sort(columns: [\"%s\"], desc: %s)\n", InfluxDbConstants.TIME_COLUMN, Objects.equals(this.order, TimeSeriesQueryOrder.ASC) ? "false" : "true"));

        if (!CollectionUtils.isEmpty(indexedColumns)) {
            sb.append(String.format("  |> sort(columns: [%s], desc: false)\n", getSortKeyColumns()));
        }

        if (limit != null) {
            sb.append(String.format("  |> limit(n: %d)\n", limit));
        }
        return sb.toString();
    }

    public String getSortKeyFilter() {
        Map<String, Object> sortKeyValues = cursor.getIndexedKeyValues();
        SearchFilter queryFilter = new SearchFilter(BooleanOperator.AND, new ArrayList<>());
        Consumer<Filterable> filterable = f1 -> f1.and(f2 -> sortKeyValues.forEach((key, value) -> f2.ge(key, value.toString())));
        filterable.accept(queryFilter);
        return buildFilterExpression(queryFilter);
    }

    public String getSortKeyColumns() {
        return indexedColumns
                .stream()
                .map(key -> "\"" + key + "\"")
                .collect(Collectors.joining(", "));
    }

    private static String buildFilterExpression(CompositeCondition condition) {
        List<String> parts = new ArrayList<>();
        for (Condition cond : condition.getConditions()) {
            if (cond instanceof CompareCondition compareCondition) {
                parts.add(buildCompareExpression(compareCondition));
            } else if (cond instanceof CompositeCondition compositeCondition) {
                String sub = buildFilterExpression(compositeCondition);
                if (!sub.isEmpty()) {
                    parts.add("(" + sub + ")");
                }
            }
        }
        String op = condition.getBooleanOperator() == BooleanOperator.AND ? " and " : " or ";
        return String.join(op, parts);
    }

    private static String buildCompareExpression(CompareCondition cond) {
        String name = formatColumnRef(StringUtils.toSnakeCase(cond.getName()));
        Object value = cond.getValue();
        SearchOperator op = cond.getSearchOperator();

        return switch (op) {
            case EQ -> name + " == " + formatValue(name, value);
            case NE -> name + " != " + formatValue(name, value);
            case GT -> name + " > " + formatValue(name, value);
            case GE -> name + " >= " + formatValue(name, value);
            case LT -> name + " < " + formatValue(name, value);
            case LE -> name + " <= " + formatValue(name, value);
            case IN -> formatIn(name, value, true);
            case NOT_IN -> formatIn(name, value, false);
            case IS_NULL -> name + " == null";
            case IS_NOT_NULL -> name + " != null";
            case LIKE, CASE_IGNORE_LIKE -> "regexp.matchRegexp(regexp: /" + value + "/, v: " + name + ")";
            case NOT_LIKE, CASE_IGNORE_NOT_LIKE -> "!regexp.matchRegexp(regexp: /" + value + "/, v: " + name + ")";
            case STARTS_WITH, CASE_IGNORE_STARTS_WITH ->
                    "strings.hasPrefix(v: " + name + ", prefix: \"" + value + "\")";
            case ENDS_WITH, CASE_IGNORE_ENDS_WITH -> "strings.hasSuffix(v: " + name + ", suffix: \"" + value + "\")";
            case BETWEEN -> {
                Pair<?, ?> pair = (Pair<?, ?>) value;
                yield name + " >= " + formatValue(name, pair.getFirst()) + " and " + name + " <= " + formatValue(name, pair.getSecond());
            }
            default -> throw new UnsupportedOperationException("Unsupported operator: " + op);
        };
    }

    private static String formatValue(String name, Object value) {
        if (RECORD_TEMP_TIME_VARIABLE.equals(name)) {
            return formatTime((Long) value);
        }

        return "\"" + value + "\"";
    }

    private static String formatColumnRef(String columnName) {
        return String.format("%s[\"%s\"]", RECORD_TEMP_VARIABLE, columnName);
    }

    private static String formatIn(String name, Object value, boolean positive) {
        Object[] arr = (Object[]) value;
        List<String> exprs = new ArrayList<>();
        for (Object v : arr) {
            exprs.add(name + " == " + formatValue(name, v));
        }
        String joined = String.join(" or ", exprs);
        return positive ? "(" + joined + ")" : "!(" + joined + ")";
    }

    private static String formatTime(Long timestamp) {
        return String.format("time(v: %s)", Duration.of(timestamp, ChronoUnit.MILLIS).toNanos());
    }

    private Consumer<Filterable> toIndexFilterable(Map<String, Object> indexedKeyValues) {
        return f1 -> f1.and(f2 -> indexedKeyValues.forEach((key, value) -> f2.eq(StringUtils.toSnakeCase(key), value)));
    }
}
