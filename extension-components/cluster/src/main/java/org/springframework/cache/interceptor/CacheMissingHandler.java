package org.springframework.cache.interceptor;

import com.milesight.beaveriot.base.annotations.cacheable.CacheKeys;
import lombok.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class CacheMissingHandler {

    private Map<String, Object> resultMap;
    private List<Integer> missingKeysIndex;
    private ProceedingJoinPoint pjp;

    public CacheMissingHandler(Map<String, Object> resultMap, List<Integer> missingKeysIndex, ProceedingJoinPoint pjp) {
        this.resultMap = resultMap;
        this.missingKeysIndex = missingKeysIndex;
        this.pjp = pjp;
    }

    @SneakyThrows
    public Cache.ValueWrapper handle() {

        if (ObjectUtils.isEmpty(missingKeysIndex)) {
            return new SimpleValueWrapper(resultMap);
        }

        Integer cacheKeyParamIdx = retrieveCacheKeysAnnotationIndex(pjp);

        // missing @CacheKeys parameter, return null, and will invoke the method again
        if (cacheKeyParamIdx == null) {
            return null;
        }

        // retry
        Object[] args = createRetryArguments(pjp, cacheKeyParamIdx, missingKeysIndex);

        Object result = pjp.proceed(args);
        if (!ObjectUtils.isEmpty(result)) {
            Assert.isTrue(result instanceof Map<?, ?>, "The result of the method annotated with @CacheKeys must be a Map, but was: " + result.getClass().getName());
            resultMap.putAll((Map<String, Object>)result);
        }

        return new MissingCacheValueWrapper(resultMap, result);
    }

    private Object[] createRetryArguments(ProceedingJoinPoint pjp, Integer cacheKeyParamIdx, List<Integer> missingKeysIndex) {
        Object[] args = pjp.getArgs();
        Object cacheKeyParam = args[cacheKeyParamIdx];
        Assert.isTrue(cacheKeyParam instanceof List<?>, "The parameter annotated with @CacheKeys must be a List, but was: " + cacheKeyParam.getClass().getName());
        AtomicInteger idx = new AtomicInteger(0);
        List<?> filterKeys = ((Collection<?>) cacheKeyParam).stream().filter(item -> missingKeysIndex.contains(idx.getAndIncrement())).collect(Collectors.toList());
        args[cacheKeyParamIdx] = filterKeys;
        return args;
    }

    @Nullable
    private Integer retrieveCacheKeysAnnotationIndex(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        return retrieveCacheKeysAnnotationIndex(method);
    }

    @Nullable
    public static Integer retrieveCacheKeysAnnotationIndex(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (annotation instanceof CacheKeys) {
                    return i;
                }
            }
        }
        return null;
    }

}
