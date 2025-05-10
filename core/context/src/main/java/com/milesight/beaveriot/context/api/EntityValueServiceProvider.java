package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import lombok.*;

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

    void saveValues(ExchangePayload exchangePayload, long timestamp);

    void saveValues(ExchangePayload exchangePayload);

    void saveHistoryRecord(Map<String, Object> recordValues, long timestamp);

    void saveHistoryRecord(Map<String, Object> recordValues);

    void mergeHistoryRecord(Map<String, Object> recordValues, long timestamp);

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
