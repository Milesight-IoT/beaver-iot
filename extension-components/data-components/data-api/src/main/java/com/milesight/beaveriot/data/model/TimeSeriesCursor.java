package com.milesight.beaveriot.data.model;

import lombok.Data;

import java.util.Map;
import java.util.TreeMap;

/**
 * author: Luxb
 * create: 2025/10/29 14:23
 **/
@Data
public class TimeSeriesCursor {
    private Long timestamp;
    private Map<String, Object> sortKeyValues;

    public void putSortKeyValue(String sortKey, Object sortKeyValue) {
        sortKeyValues.put(sortKey, sortKeyValue);
    }

    private TimeSeriesCursor() {
        sortKeyValues = new TreeMap<>();
    }

    public static class Builder {
        private final TimeSeriesCursor cursor = new TimeSeriesCursor();

        public Builder(Long timestamp) {
            this.timestamp(timestamp);
        }

        public Builder timestamp(Long timestamp) {
            cursor.setTimestamp(timestamp);
            return this;
        }

        public Builder putSortKeyValue(String sortKey, Object sortKeyValue) {
            cursor.putSortKeyValue(sortKey, sortKeyValue);
            return this;
        }

        public TimeSeriesCursor build() {
            return cursor;
        }
    }
}