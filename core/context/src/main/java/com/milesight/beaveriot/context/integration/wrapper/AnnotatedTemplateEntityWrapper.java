package com.milesight.beaveriot.context.integration.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.annotations.SFunction;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class AnnotatedTemplateEntityWrapper<T> extends AbstractWrapper {

    private String deviceIdentify;

    public AnnotatedTemplateEntityWrapper(String deviceIdentify) {
        this.deviceIdentify = deviceIdentify;
    }

    private ExchangePayload exchangePayload;

    @Override
    protected <S> String parserEntityKey(SFunction<S, ?> keyFun) {

        String entityKeyTemplate = super.parserEntityKey(keyFun);

        return MessageFormat.format(entityKeyTemplate, deviceIdentify);
    }

    public ExchangeEventPublisher saveValue(SFunction<T, ?> keyFun, Object value, long timestamp) {

        String key = parserEntityKey(keyFun);

        exchangePayload = ExchangePayload.create(key, value);

        doSaveValue(exchangePayload, timestamp);

        return new ExchangeEventPublisher(exchangePayload);
    }

    public ExchangeEventPublisher saveValue(SFunction<T, ?> keyFun, Object value) {

        return saveValue(keyFun, value, System.currentTimeMillis());
    }

    public ExchangeEventPublisher saveValues(Map<SFunction<T, ?>, Object> values) {

        return saveValues(values, System.currentTimeMillis());
    }

    public ExchangeEventPublisher saveValues(Map<SFunction<T, ?>, Object> values, long timestamp) {

        Map<String, Object> collect = values.entrySet().stream().collect(
                Collectors.toMap(entry -> parserEntityKey(entry.getKey()), Map.Entry::getValue));

        exchangePayload = ExchangePayload.create(collect);

        doSaveValue(exchangePayload, timestamp);

        return new ExchangeEventPublisher(exchangePayload);
    }

    public <S> Optional<S> getValue(SFunction<T, ?> keyFun, Class<S> clazz) {
        return findValueByKey(parserEntityKey(keyFun), clazz);
    }

    public Map<String, JsonNode> getValues(SFunction<T, ?>... parentKeyFun) {
        List<String> keys = Arrays.stream(parentKeyFun).map(this::parserEntityKey).toList();
        return findValuesByKeys(keys);
    }

//    public Map<String, JsonNode> getChildValues(SFunction<T,?> parentKeyFun) {
//    }
}
