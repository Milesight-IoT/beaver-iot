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
    private Map<String, Object> indexedKeyValues;

    public static TimeSeriesCursor of(Long timestamp, Map<String, Object> indexedKeyValues) {
        TimeSeriesCursor cursor = new TimeSeriesCursor();
        cursor.setTimestamp(timestamp);
        cursor.setIndexedKeyValues(indexedKeyValues);
        return cursor;
    }

    public void putIndexedKeyValue(String indexedKey, Object value) {
        indexedKeyValues.put(indexedKey, value);
    }

    private TimeSeriesCursor() {
        indexedKeyValues = new TreeMap<>();
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

        @SuppressWarnings("UnusedReturnValue")
        public Builder putIndexedKeyValues(String indexedKey, Object value) {
            cursor.putIndexedKeyValue(indexedKey, value);
            return this;
        }

        public TimeSeriesCursor build() {
            return cursor;
        }
    }
}