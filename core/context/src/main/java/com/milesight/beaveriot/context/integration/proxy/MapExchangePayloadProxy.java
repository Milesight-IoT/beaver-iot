package com.milesight.beaveriot.context.integration.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.KeyPatternMatcher;
import com.milesight.beaveriot.context.integration.entity.annotation.AnnotationEntityCache;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author leon
 */
public class MapExchangePayloadProxy<T extends ExchangePayload> {
    private final Class<T> parameterType;
    private final Map allPayloads;

    /**
     * payload prefix ，Used to filter subpayloads, which may contain characters  {0}
     */
    private String payloadPrefix;
    private static final ConversionService conversionService = ApplicationConversionService.getSharedInstance();

    public <P> MapExchangePayloadProxy(Map<String, P> allPayloads, Class<T> parameterType) {
        Assert.notNull(allPayloads, "allPayloads must not be null");
        this.allPayloads = allPayloads;
        this.parameterType = parameterType;
    }

    public <P> MapExchangePayloadProxy(Map<String, P> allPayloads, Class<T> parameterType, String payloadPrefix) {
        this(allPayloads, parameterType);
        this.payloadPrefix = payloadPrefix;
    }

    public T proxy() {
        ProxyFactory factory = new ProxyFactory();
        T newInstance = newInstance(parameterType);
        factory.setTarget(newInstance);
        factory.addAdvice((MethodInterceptor) invocation -> {

            Method method = invocation.getMethod();
            if ("toString".equals(method.getName())) {
                return allPayloads.toString();
            } else if ("hashCode".equals(method.getName()) || "equals".equals(method.getName())) {
                return invocation.proceed();
            }

            String cacheEntityKey = AnnotationEntityCache.INSTANCE.getEntityKeyByMethod(method);
            if (cacheEntityKey != null) {
                if(method.getName().startsWith("set")){
                    Assert.isTrue(!cacheEntityKey.contains(StringConstant.FORMAT_PLACE_HOLDER), "@DeviceTemplateEntity annotated entities cannot be used for set value");
                    Object argument = invocation.getArguments()[0];
                    newInstance.put(cacheEntityKey, argument);
                    //also set original exchangePayload value
                    allPayloads.put(cacheEntityKey, argument);
                }else{
                    if(ExchangePayload.class.isAssignableFrom(method.getReturnType())){
                        return new MapExchangePayloadProxy<>(newInstance, (Class<? extends ExchangePayload>) method.getReturnType(), cacheEntityKey).proxy();
                    }else{
                        return parserValue(getMatchValue(newInstance, cacheEntityKey), method.getReturnType());
                    }
                }
            }

            return invocation.proceed();
        });
        //noinspection unchecked
        return (T) factory.getProxy();
    }

    private Object getMatchValue(T newInstance, String cacheEntityKey) {
        //Support wildcard value method
        if(cacheEntityKey.contains(StringConstant.FORMAT_PLACE_HOLDER)){
            String matchPattern = cacheEntityKey.replace(StringConstant.FORMAT_PLACE_HOLDER, StringConstant.STAR);
            Optional<Map.Entry<String, Object>> first = newInstance.entrySet().stream().filter(entry -> KeyPatternMatcher.match(matchPattern, entry.getKey())).findFirst();
            if(first.isPresent()){
                return first.get().getValue();
            }
        }
        return newInstance.get(cacheEntityKey);
    }

    private Map<String, Object> filterChildPayload(Map<String,Object> payloads, String prefixKey) {
        Map<String, Object> filteredPayloads = new LinkedHashMap<>();
        payloads.forEach((key, value) -> {
            boolean isMatch = prefixKey.contains(StringConstant.FORMAT_PLACE_HOLDER) ?
                            KeyPatternMatcher.match(prefixKey.replace(StringConstant.FORMAT_PLACE_HOLDER, StringConstant.STAR) + StringConstant.STAR, key) :
                            key.startsWith(prefixKey);
            if(isMatch){
                filteredPayloads.put(key, value);
            }
        });
        return filteredPayloads;
    }

    protected Object parserValue(Object value, Class<?> returnType) {
        if(ObjectUtils.isEmpty(value)){
            return value;
        }
        if (!ObjectUtils.isEmpty(value) && value instanceof JsonNode) {
            return JsonUtils.cast(value, returnType);
        }
        return conversionService.convert(value, returnType);
    }

    @SneakyThrows
    protected T newInstance(Class<T> parameterType) {
        T payload = parameterType.getDeclaredConstructor().newInstance();
        if(ObjectUtils.isEmpty(payloadPrefix)){
            payload.putAll(allPayloads);
        }else{
            payload.putAll(filterChildPayload(allPayloads, payloadPrefix));
        }
        return payload;
    }
}
