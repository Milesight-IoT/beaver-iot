package com.milesight.beaveriot.data.model;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.data.filterable.Filterable;
import lombok.Data;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TimeSeriesBaseQuery class.
 *
 * @author simon
 * @date 2025/10/13
 */
@Data
public class TimeSeriesBaseQuery {
    private Map<String, Object> indexedKeyValues;
    private Consumer<Filterable> filterable;

    public void validate(Collection<String> indexedKeys) {
        if (indexedKeyValues == null) {
            throw new IllegalArgumentException("indexedKeyValues cannot be null");
        }

        Set<String> actualKeys = indexedKeyValues.keySet().stream().map(StringUtils::toCamelCase).collect(Collectors.toSet());
        Set<String> expectedKeys = indexedKeys.stream().map(StringUtils::toCamelCase).collect(Collectors.toSet());

        if (!actualKeys.equals(expectedKeys)) {
            throw new IllegalArgumentException(
                    String.format("indexedKeyValues keys mismatch. Expected: %s, Actual: %s",
                            expectedKeys, actualKeys)
            );
        }

        for (String key : indexedKeyValues.keySet()) {
            Object value = indexedKeyValues.get(key);
            if (value == null) {
                throw new IllegalArgumentException(
                        String.format("Value for indexed key '%s' cannot be null", key)
                );
            }
        }
    }
}