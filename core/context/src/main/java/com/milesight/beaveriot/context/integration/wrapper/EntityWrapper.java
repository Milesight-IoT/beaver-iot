package com.milesight.beaveriot.context.integration.wrapper;

import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class EntityWrapper extends AbstractWrapper {

    private final Entity entity;

    private ExchangePayload exchangePayload;

    public EntityWrapper(Entity entity) {
        this.entity = entity;
    }

    public ExchangeEventPublisher saveValue(Object value, long timestamp) {

        exchangePayload = ExchangePayload.create(entity.getKey(), value);

        doSaveValue(exchangePayload, timestamp);

        return new ExchangeEventPublisher(exchangePayload);
    }

    public ExchangeEventPublisher saveValue(Object value) {

        return saveValue(value, System.currentTimeMillis());
    }

    public ExchangeEventPublisher saveValues(Map<String, Object> values) {

        return saveValues(values, System.currentTimeMillis());
    }

    private ExchangeEventPublisher doSaveValues(Map<String, Object> values, long timestamp) {

        exchangePayload = ExchangePayload.create(values);

        doSaveValue(exchangePayload, timestamp);

        return new ExchangeEventPublisher(exchangePayload);
    }

    public ExchangeEventPublisher saveValues(Map<String, Object> values, long timestamp) {

        Map<String, Object> convertedValues = entity.getChildren()
                            .stream()
                            .filter(entity -> values.containsKey(entity.getIdentifier()))
                            .collect(Collectors.toMap(Entity::getKey, entity -> values.get(entity.getIdentifier())));

        Assert.isTrue(convertedValues.size() == values.size(), "Entity identifier are not valid, must be child entity identifier: " + values.keySet());

        return doSaveValues(convertedValues, timestamp);
    }

    public Optional<Object> getValue() {
        return findValueByKey(entity.getKey());
    }

}
