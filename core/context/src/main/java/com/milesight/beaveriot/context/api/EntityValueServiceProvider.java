package com.milesight.beaveriot.context.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author leon
 */
public interface EntityValueServiceProvider {

    void saveValuesAndPublish(ExchangePayload exchangePayload);

    void saveValuesAndPublish(ExchangePayload exchangePayload, Consumer<EventResponse> consumer);

    void saveValuesAndPublish(ExchangePayload exchangePayload, String eventType);

    void saveValuesAndPublish(ExchangePayload exchangePayload, String eventType, Consumer<EventResponse> consumer);

    void saveValues(Map<String, Object> values, long timestamp);

    void saveValues(Map<String, Object> values);

    void saveHistoryRecord(Map<String, Object> recordValues, long timestamp);

    void saveHistoryRecord(Map<String, Object> recordValues);

    JsonNode findValueByKey(String key);

    Map<String, JsonNode> findValuesByKeys(List<String> keys);

    @NonNull <T extends ExchangePayload> T findValuesByKey(String key, Class<T> entitiesClazz);

}
