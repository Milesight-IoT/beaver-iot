package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

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

    Object findValueByKey(String key);

    Map<String, Object> findValuesByKeys(List<String> keys);

    @NonNull <T extends ExchangePayload> T findValuesByKey(String key, Class<T> entitiesClazz);

}
