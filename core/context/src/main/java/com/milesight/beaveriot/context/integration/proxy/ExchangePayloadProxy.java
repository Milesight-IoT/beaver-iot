package com.milesight.beaveriot.context.integration.proxy;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.SneakyThrows;

/**
 * @author leon
 */
public class ExchangePayloadProxy<T extends ExchangePayload> extends MapExchangePayloadProxy<T> {
    private final ExchangePayload exchangePayload;

    public ExchangePayloadProxy(ExchangePayload exchangePayload, Class<T> parameterType) {
        super(exchangePayload.getAllPayloads(), parameterType);
        this.exchangePayload = exchangePayload;
    }

    @Override
    @SneakyThrows
    protected T newInstance(Class<T> parameterType) {
        T newInstance = super.newInstance(parameterType);
        newInstance.setContext(exchangePayload.getContext());
        newInstance.setTimestamp(exchangePayload.getTimestamp());
        return newInstance;
    }

}
