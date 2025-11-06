package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import lombok.*;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author leon
 */
public interface EntityValueServiceProvider {

    EventResponse saveValuesAndPublishSync(ExchangePayload exchangePayload);

    void saveValuesAndPublishAsync(ExchangePayload exchangePayload);

    EventResponse saveValuesAndPublishSync(ExchangePayload exchangePayload, String eventType);

    void saveValuesAndPublishAsync(ExchangePayload exchangePayload, String eventType);

    Map<String, Long> saveLatestValues(ExchangePayload exchangePayload);

    Map<String, Pair<Long, Long>> saveValues(ExchangePayload exchangePayload, long timestamp);

    Map<String, Pair<Long, Long>> saveValues(ExchangePayload exchangePayload);

    Map<String, Long> saveHistoryRecord(Map<String, Object> recordValues, long timestamp);

    Map<String, Long> saveHistoryRecord(Map<String, Object> recordValues);

    Map<String, Long> mergeHistoryRecord(Map<String, Object> recordValues, long timestamp);

    /**
     * Check if the entity history records exist
     *
     * @param keys      Entity keys of the history record
     * @param timestamp Timestamp of the history record
     * @return Entity keys of the existing history record
     */
    Set<String> existHistoryRecord(Set<String> keys, long timestamp);

    boolean existHistoryRecord(String key, long timestamp);

    Object findValueByKey(String key);

    Map<String, Object> findValuesByKeys(List<String> keys);

    @NonNull <T extends ExchangePayload> T findValuesByKey(String key, Class<T> entitiesClazz);

}
