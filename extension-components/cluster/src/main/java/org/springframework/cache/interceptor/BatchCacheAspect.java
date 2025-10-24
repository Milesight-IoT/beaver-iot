package org.springframework.cache.interceptor;

import com.milesight.beaveriot.cache.BatchableCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.operation.BatchCacheEvictOperation;
import org.springframework.cache.interceptor.operation.BatchCachePutOperation;
import org.springframework.cache.interceptor.operation.BatchCacheableOperation;
import org.springframework.cache.interceptor.operation.CacheKeyPrefix;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author leon
 */
@Slf4j
@Aspect
public class BatchCacheAspect extends AbstractCacheInvoker implements BeanFactoryAware {

    private static final Collection<?> NO_RESULT = new ArrayList<>();

    private final StandardEvaluationContext originalEvaluationContext = new StandardEvaluationContext();

    private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator(new CacheEvaluationContextFactory(this.originalEvaluationContext));

    private final Map<BatchCacheOperationCacheKey, BatchCacheOperationMetadata> metadataCache = new ConcurrentHashMap<>(1024);

    private SingletonSupplier<KeyGenerator> keyGenerator = SingletonSupplier.of(SimpleKeyGenerator::new);

    private final CacheOperationSource cacheOperationSource;
    private BeanFactory beanFactory;

    public BatchCacheAspect() {
        this.cacheOperationSource = new AnnotationCacheOperationSource(new SpringBatchCacheAnnotationParser());
    }

    @Around(value = "@annotation(com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict) || " +
            "@annotation(com.milesight.beaveriot.base.annotations.cacheable.BatchCachePut) ||" +
            "@annotation(com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable) ||" +
            "@annotation(com.milesight.beaveriot.base.annotations.cacheable.BatchCaching)")
    public Object execute(ProceedingJoinPoint pjp) throws Throwable {

        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, pjp.getTarget().getClass());

        BatchCacheOperationContext evictOperationContext = createBatchCacheOperationContextIfPresent(method, pjp.getArgs(), pjp.getTarget().getClass(), operations, BatchCacheEvictOperation.class);
        beforeProcessCacheEvicts(evictOperationContext, CacheOperationExpressionEvaluator.NO_RESULT);

        // Check if we have a cached value matching the conditions
        BatchCacheOperationContext cacheableOperationContext = createBatchCacheOperationContextIfPresent(method, pjp.getArgs(), pjp.getTarget().getClass(), operations, BatchCacheableOperation.class);
        Object cacheHit = findCachedValue(cacheableOperationContext, pjp);

        // Check if we have a cached value matching the conditions
        if (cacheHit == null || cacheHit instanceof Cache.ValueWrapper) {
            BatchCacheOperationContext putOperationContext = createBatchCacheOperationContextIfPresent(method, pjp.getArgs(), pjp.getTarget().getClass(), operations, BatchCachePutOperation.class);
            return this.evaluate(pjp, method, cacheHit, evictOperationContext, putOperationContext, cacheableOperationContext);
        }
        return unwrapReturnValue(cacheHit);
    }

    private Object evaluate(ProceedingJoinPoint pjp, Method method, @Nullable Object cacheHit, @Nullable BatchCacheOperationContext evictOperationContext, @Nullable BatchCacheOperationContext putOperationContext, @Nullable BatchCacheOperationContext cacheableOperationContext) throws Throwable {

        Object cacheValue;
        Object returnValue;
        if (cacheHit != null && putOperationContext == null) {
            // If there are no put requests, just use the cache hit
            cacheValue = unwrapCacheValue(cacheHit);
            returnValue = wrapCacheValue(method, cacheValue);
        } else {
            // Invoke the method if we don't have a cache hit
            returnValue = pjp.proceed();
            cacheValue = unwrapReturnValue(returnValue);
        }

        // Collect puts from any @Cacheable miss, if no cached value is found
        if (cacheableOperationContext != null) {
            if (cacheHit == null) {
                putCacheableRequest(cacheableOperationContext, cacheValue);
            } else if (cacheHit instanceof MissingCacheValueWrapper missingCacheValueWrapper) {
                putCacheableRequest(cacheableOperationContext, missingCacheValueWrapper.getMissingCacheValue());
            }
        }

        // Collect puts from any @BatchCachePut operation, if present
        collectPutCacheRequest(putOperationContext, cacheValue);

        afterProcessCacheEvicts(evictOperationContext, cacheValue, method);

        return returnValue;
    }

    private void collectPutCacheRequest(@Nullable BatchCacheOperationContext putCacheOperationContext, Object cacheValue) {
        if (ObjectUtils.isEmpty(cacheValue) || putCacheOperationContext == null) {
            return;
        }
        Collection<?> cacheValueCollection = validateResultAndConvertToCollection(putCacheOperationContext, cacheValue);

        FullKeyHolder keyHolder = cacheValue instanceof Map<?,?> cacheValueMap ?
                FullKeyHolder.of(cacheValueMap.keySet(), putCacheOperationContext.generateKeyPrefix(putCacheOperationContext.createEvaluationContext(cacheValue))) :
                putCacheOperationContext.generateKey(cacheValueCollection);
        Object keys = keyHolder.getKeys();
        if (ObjectUtils.isEmpty(keys)) {
            return;
        }
        Assert.isTrue(keys instanceof List<?>, "BatchCache operation can only put cache entries for Collection or array key types");
        AtomicInteger i = new AtomicInteger(0);
        if (putCacheOperationContext.isConditionPassing(cacheValueCollection)) {
            Assert.isTrue(((List) keys).size() == cacheValueCollection.size(),
                    "BatchCache operation with key return cache size not equal to the cacheValue size, Not put caches for method " + putCacheOperationContext.getMethod().getName());
            for (Object cacheValueItem : cacheValueCollection) {
                Object key = ((List) keys).get(i.getAndIncrement());
                if (!ObjectUtils.isEmpty(key)) {
                    BatchCachePutRequest batchCachePutRequest = new BatchCachePutRequest(putCacheOperationContext, key);
                    batchCachePutRequest.apply(cacheValueItem);
                }
            }
        }
    }

    private void putCacheableRequest(BatchCacheOperationContext cacheableOperationContext, @Nullable Object cacheValue) {
        if (ObjectUtils.isEmpty(cacheValue) || cacheableOperationContext == null) {
            return;
        }
        Collection<?> cacheValueCollection = validateResultAndConvertToCollection(cacheableOperationContext, cacheValue);
        if (cacheableOperationContext.isConditionPassing(cacheValue)) {
            FullKeyHolder keyHolder = cacheValue instanceof Map<?,?> cacheValueMap ?
                    FullKeyHolder.of(cacheValueMap.keySet(), cacheableOperationContext.generateKeyPrefix(cacheableOperationContext.createEvaluationContext(cacheValue))) :
                    cacheableOperationContext.generateKey(cacheValue);
            Object[] putCacheKeys = validateKeyAndConvertToArray(keyHolder.getKeys());
            if (!ObjectUtils.isEmpty(putCacheKeys) && putCacheKeys.length == cacheValueCollection.size()) {
                int idx = 0;
                for (Object cacheValueItem : cacheValueCollection) {
                    BatchCachePutRequest batchCachePutRequest = new BatchCachePutRequest(cacheableOperationContext, putCacheKeys[idx]);
                    batchCachePutRequest.apply(cacheValueItem);
                    idx++;
                }
            } else {
                log.info("BatchCacheable operation with key return cache size not equal to the cacheValue size, " +
                        "Not put caches for method {} ", cacheableOperationContext.getMethod().getName());
            }
        }

    }

    private void beforeProcessCacheEvicts(@Nullable BatchCacheOperationContext context, @Nullable Object result) {
        if (ObjectUtils.isEmpty(context)) {
            return;
        }
        if (!((BatchCacheEvictOperation) context.getOperation()).isBeforeInvocation()) {
            return;
        }
        FullKeyHolder fullKeyHolder = context.generateKey(result);
        Object[] evictKeyArray = validateKeyAndConvertToArray(fullKeyHolder.getKeys());
        for (Cache cache : context.getCaches()) {
            if (context.isConditionPassing(result)) {
                doCacheEvict(context, cache, evictKeyArray);
            }
        }
    }

    private void doCacheEvict(BatchCacheOperationContext context, Cache cache, Object[] evictKeyArray) {
        if (((BatchCacheEvictOperation) context.getOperation()).isCacheWide()) {
            doClear(cache, true);
        } else {
            if (cache instanceof BatchableCache) {
                ((BatchableCache) cache).multiEvict(evictKeyArray);
            } else {
                for (Object key : evictKeyArray) {
                    if (!ObjectUtils.isEmpty(key)) {
                        doEvict(cache, key, false);
                    }
                }
            }
        }
    }

    private void afterProcessCacheEvicts(@Nullable BatchCacheOperationContext context, @Nullable Object result, Method method) {
        if ((ObjectUtils.isEmpty(context) || ObjectUtils.isEmpty(result)) && !ClassUtils.isVoidType(method.getReturnType())) {
            return;
        }
        BatchCacheEvictOperation operation = (BatchCacheEvictOperation) context.getOperation();
        if (operation.isBeforeInvocation()) {
            return;
        }

        Collection<?> cacheValueCollection = validateResultAndConvertToCollection(context, result);
        FullKeyHolder fullKeyHolder = context.generateKey(cacheValueCollection);
        Object keys = fullKeyHolder.getKeys();
        if (ObjectUtils.isEmpty(keys)) {
            return;
        }
        Assert.isTrue(keys instanceof List<?>, "BatchCache operation can only put cache entries for Collection or array key types");
        if (context.isConditionPassing(cacheValueCollection)) {
            for (Cache cache : context.getCaches()) {
                doCacheEvict(context, cache, validateKeyAndConvertToArray(keys));
            }
        }
    }

    @Nullable
    private Object findCachedValue(@Nullable BatchCacheOperationContext cacheableOperationContext, ProceedingJoinPoint pjp) {
        if (cacheableOperationContext == null) {
            return null;
        }

        if (cacheableOperationContext.isConditionPassing(CacheOperationExpressionEvaluator.NO_RESULT)) {
            FullKeyHolder fullKeyHolder = cacheableOperationContext.generateKey(CacheOperationExpressionEvaluator.NO_RESULT);
            Object cached = findInCaches(cacheableOperationContext, fullKeyHolder, pjp);
            if (cached != null) {
                log.trace("Cache entry for key '" + fullKeyHolder.getKeys() + "' found in cache(s) {} ", cacheableOperationContext.getCacheNames());
                return cacheableOperationContext.canApplyCache(cached) ? cached : null;
            } else {
                log.trace("No cache entry for key '" + fullKeyHolder.getKeys() + "' in cache(s) " + cacheableOperationContext.getCacheNames());
            }
        }
        return null;
    }

    @Nullable
    protected BatchCacheOperationContext createBatchCacheOperationContextIfPresent(Method method, Object[] args, Object target, @Nullable Collection<CacheOperation> cacheOperations, Class<? extends CacheOperation> operationClass) {
        if (ObjectUtils.isEmpty(cacheOperations)) {
            return null;
        }
        Optional<CacheOperation> cacheOperationOptional = cacheOperations.stream().filter(operationClass::isInstance).findFirst();
        return cacheOperationOptional.isPresent() ? new BatchCacheOperationContext(getCacheOperationMetadata(cacheOperationOptional.get(), method, target.getClass()), args, target) : null;
    }

    @Nullable
    private Object findInCaches(BatchCacheOperationContext context, FullKeyHolder keyHolder, ProceedingJoinPoint pjp) {

        Object keys = keyHolder.getKeys();
        if (ObjectUtils.isEmpty(keys)) {
            return null;
        }

        Object[] keyArray = validateKeyAndConvertToArray(keys);
        for (Cache cache : context.getCaches()) {
            Class<?> returnType = context.getMethod().getReturnType();
            if (Collection.class.isAssignableFrom(returnType) || returnType.isArray()) {
                List<?> results = doFindInCache(keyArray, cache).stream().filter(Objects::nonNull).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(results) || results.size() < keyArray.length) {
                    log.debug("BatchCacheable operation with key [{}] return cache size not equal to the key size, Not matched all caches", Arrays.toString(keyArray));
                    return null;
                }
                return (returnType.isArray()) ? new SimpleValueWrapper(results.toArray((Object[]::new))) : new SimpleValueWrapper(results);
            } else if (Map.class.isAssignableFrom(returnType)) {
                Map<String, Object> resultMap = new LinkedHashMap<>();
                List<?> results = doFindInCache(keyArray, cache);
                int i = 0;
                List<Integer> missingKeys = new ArrayList<>();
                for (Object item : keyArray) {
                    if (!ObjectUtils.isEmpty(item)) {
                        Object cacheValue = results.get(i);
                        if (cacheValue != null) {
                            resultMap.put(FullKeyHolder.removeKeyPrefix(keyHolder.getPrefix(), item.toString()), results.get(i));
                        } else {
                            missingKeys.add(i);
                        }
                    }
                    i++;
                }
                //Retrieve missed cache value
                return new CacheMissingHandler(resultMap, missingKeys, pjp).handle();
            } else {
                throw new IllegalStateException("Batch cache operation with key of type " + keys.getClass().getName() +
                        " must return a Collection,Map or an array, but method " + context.getMethod().getName() + " returns " + returnType.getName());
            }
        }
        return null;
    }

    private List<?> doFindInCache(Object[] keyArray, Cache cache) {
        if (cache instanceof BatchableCache) {
            return ((BatchableCache) cache).multiGet(keyArray);
        } else {
            return Arrays.stream(keyArray).map(item -> doGet(cache, item)).map(wrapper -> wrapper == null ? null : wrapper.get()).toList();
        }
    }

    private Object[] validateKeyAndConvertToArray(@NonNull Object result) {
        Assert.isTrue(result instanceof Collection<?> || result.getClass().isArray(),
                "BatchCache operation can only find cache entries for Collection or array key types, but got:" + result.getClass().getName());
        return (result instanceof Collection<?> collection ? collection.toArray() : (Object[]) result);
    }

    private Collection<?> validateResultAndConvertToCollection(BatchCacheOperationContext context, @Nullable Object result) {
        if (result == null) {
            return NO_RESULT;
        } else if (result instanceof Map<?, ?>) {
            return ((Map<?, ?>) result).values();
        } else if (result.getClass().isArray()) {
            return Arrays.asList((Object[]) result);
        } else if (result instanceof Collection<?>) {
            return (Collection<?>) result;
        } else {
            String resultType = result.getClass().getName();
            throw new IllegalStateException("BatchCache operation with key of type " + resultType +
                    " must return a Collection, Map or an array, but method " + context.getMethod().getName() +
                    " returns " + resultType);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    protected <T> T getBean(String beanName, Class<T> expectedType) {
        if (this.beanFactory == null) {
            throw new IllegalStateException(
                    "BeanFactory must be set on cache aspect for " + expectedType.getSimpleName() + " retrieval");
        }
        return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, expectedType, beanName);
    }

    protected <T> T getBean(Class<T> expectedType) {
        if (this.beanFactory == null) {
            throw new IllegalStateException(
                    "BeanFactory must be set on cache aspect for " + expectedType.getSimpleName() + " retrieval");
        }
        return this.beanFactory.getBean(expectedType);
    }

    @Nullable
    private Object unwrapReturnValue(Object returnValue) {
        return ObjectUtils.unwrapOptional(returnValue);
    }

    @Nullable
    private Object unwrapCacheValue(@Nullable Object cacheValue) {
        return (cacheValue instanceof Cache.ValueWrapper wrapper ? wrapper.get() : cacheValue);
    }

    @Nullable
    private Object wrapCacheValue(Method method, @Nullable Object cacheValue) {
        if (method.getReturnType() == Optional.class &&
                (cacheValue == null || cacheValue.getClass() != Optional.class)) {
            return Optional.ofNullable(cacheValue);
        }
        return cacheValue;
    }

    protected Collection<? extends Cache> getCaches(
            CacheOperationInvocationContext<CacheOperation> context, CacheResolver cacheResolver) {

        Collection<? extends Cache> caches = cacheResolver.resolveCaches(context);
        if (caches.isEmpty()) {
            throw new IllegalStateException("No cache could be resolved for '" +
                    context.getOperation() + "' using resolver '" + cacheResolver +
                    "'. At least one cache should be provided per cache operation.");
        }
        return caches;
    }

    protected BatchCacheOperationMetadata getCacheOperationMetadata(
            CacheOperation operation, Method method, Class<?> targetClass) {

        BatchCacheOperationCacheKey cacheKey = new BatchCacheOperationCacheKey(operation, method, targetClass);
        BatchCacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
        if (metadata == null) {
            KeyGenerator operationKeyGenerator;
            if (StringUtils.hasText(operation.getKeyGenerator())) {
                operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
            } else {
                operationKeyGenerator = getKeyGenerator();
            }
            CacheResolver operationCacheResolver;
            if (StringUtils.hasText(operation.getCacheResolver())) {
                operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
            } else if (StringUtils.hasText(operation.getCacheManager())) {
                CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
                operationCacheResolver = new SimpleCacheResolver(cacheManager);
            } else {
                CacheManager cacheManager = getBean(CacheManager.class);
                operationCacheResolver = new SimpleCacheResolver(cacheManager);
                Assert.state(operationCacheResolver != null, "No CacheResolver/CacheManager set");
            }
            metadata = new BatchCacheOperationMetadata(operation, method, targetClass,
                    operationKeyGenerator, operationCacheResolver);
            this.metadataCache.put(cacheKey, metadata);
        }
        return metadata;
    }

    public KeyGenerator getKeyGenerator() {
        return this.keyGenerator.obtain();
    }

    protected class BatchCachePutRequest {

        private final BatchCacheOperationContext context;

        private final Object key;

        public BatchCachePutRequest(BatchCacheOperationContext context, Object key) {
            this.context = context;
            this.key = key;
        }

        public void apply(@Nullable Object result) {
            if (this.context.canApplyCache(result)) {
                for (Cache cache : this.context.getCaches()) {
                    doPut(cache, this.key, result);
                }
            }
        }
    }

    protected class BatchCacheOperationMetadata {

        private final CacheOperation operation;

        private final Method method;

        private final Class<?> targetClass;

        private final Method targetMethod;

        private final AnnotatedElementKey methodKey;

        private final KeyGenerator keyGenerator;

        private final CacheResolver cacheResolver;

        public BatchCacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass,
                                           KeyGenerator keyGenerator, CacheResolver cacheResolver) {

            this.operation = operation;
            this.method = BridgeMethodResolver.findBridgedMethod(method);
            this.targetClass = targetClass;
            this.targetMethod = (!Proxy.isProxyClass(targetClass) ?
                    AopUtils.getMostSpecificMethod(method, targetClass) : this.method);
            this.methodKey = new AnnotatedElementKey(this.targetMethod, targetClass);
            this.keyGenerator = keyGenerator;
            this.cacheResolver = cacheResolver;
        }
    }

    public final class BatchCacheOperationCacheKey implements Comparable<BatchCacheOperationCacheKey> {

        private final CacheOperation cacheOperation;

        private final AnnotatedElementKey methodCacheKey;

        public BatchCacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
            this.cacheOperation = cacheOperation;
            this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BatchCacheOperationCacheKey)) {
                return false;
            }
            BatchCacheOperationCacheKey otherKey = (BatchCacheOperationCacheKey) other;
            return (this.cacheOperation.equals(otherKey.cacheOperation) &&
                    this.methodCacheKey.equals(otherKey.methodCacheKey));
        }

        @Override
        public int hashCode() {
            return (this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode());
        }

        @Override
        public String toString() {
            return this.cacheOperation + " on " + this.methodCacheKey;
        }

        @Override
        public int compareTo(BatchCacheOperationCacheKey other) {
            int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
            if (result == 0) {
                result = this.methodCacheKey.compareTo(other.methodCacheKey);
            }
            return result;
        }
    }

    public class BatchCacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {

        private final BatchCacheOperationMetadata metadata;

        private final Object[] args;

        private final Object target;

        private final Collection<? extends Cache> caches;

        private final Collection<String> cacheNames;

        @Nullable
        private Boolean conditionPassing;

        public BatchCacheOperationContext(BatchCacheOperationMetadata metadata, Object[] args, Object target) {
            this.metadata = metadata;
            this.args = extractArgs(metadata.method, args);
            this.target = target;
            this.caches = BatchCacheAspect.this.getCaches(this, metadata.cacheResolver);
            this.cacheNames = createCacheNames(this.caches);
        }

        @Override
        public CacheOperation getOperation() {
            return this.metadata.operation;
        }

        @Override
        public Object getTarget() {
            return this.target;
        }

        @Override
        public Method getMethod() {
            return this.metadata.method;
        }

        @Override
        public Object[] getArgs() {
            return this.args;
        }

        private Object[] extractArgs(Method method, Object[] args) {
            if (!method.isVarArgs()) {
                return args;
            }
            Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
            Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
            System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
            System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
            return combinedArgs;
        }

        protected boolean isConditionPassing(@Nullable Object result) {
            if (this.conditionPassing == null) {
                if (StringUtils.hasText(this.metadata.operation.getCondition())) {
                    EvaluationContext evaluationContext = createEvaluationContext(result);
                    this.conditionPassing = evaluator.condition(this.metadata.operation.getCondition(),
                            this.metadata.methodKey, evaluationContext);
                } else {
                    this.conditionPassing = true;
                }
            }
            return this.conditionPassing;
        }

        protected boolean canApplyCache(@Nullable Object value) {
            String unless = "";
            if (this.metadata.operation instanceof BatchCachePutOperation batchCachePutOperation) {
                unless = batchCachePutOperation.getUnless();
            } else if (this.metadata.operation instanceof BatchCacheableOperation batchCacheableOperation) {
                unless = batchCacheableOperation.getUnless();
            }
            if (StringUtils.hasText(unless)) {
                EvaluationContext evaluationContext = createEvaluationContext(value);
                return !evaluator.unless(unless, this.metadata.methodKey, evaluationContext);
            }
            return true;
        }

        /**
         * Compute the key for the given caching operation.
         */
        @Nullable
        protected FullKeyHolder generateKey(@Nullable Object result) {
            if (result == NO_RESULT) {
                result = CacheOperationExpressionEvaluator.NO_RESULT;
            }

            if (StringUtils.hasText(this.metadata.operation.getKey())) {
                EvaluationContext evaluationContext = createEvaluationContext(result);
                Object key = evaluator.key(this.metadata.operation.getKey(), this.metadata.methodKey, evaluationContext);
                String prefix = generateKeyPrefix(evaluationContext);
                if (ObjectUtils.isEmpty(prefix) || ObjectUtils.isEmpty(key)) {
                    return FullKeyHolder.of(key);
                } else {
                    return FullKeyHolder.of(key, prefix);
                }
            }
            return FullKeyHolder.of(this.metadata.keyGenerator.generate(this.target, this.metadata.method, this.args));
        }

        protected String generateKeyPrefix(EvaluationContext evaluationContext) {
            if (this.metadata.operation instanceof CacheKeyPrefix cacheKeyPrefix && StringUtils.hasText(cacheKeyPrefix.getKeyPrefix())) {
                Object prefix = evaluator.key(cacheKeyPrefix.getKeyPrefix(), this.metadata.methodKey, evaluationContext);
                return ObjectUtils.isEmpty(prefix) ? "" : prefix.toString();
            }
            return null;
        }

        private EvaluationContext createEvaluationContext(@Nullable Object result) {
            return evaluator.createEvaluationContext(this.caches, this.metadata.method, this.args,
                    this.target, this.metadata.targetClass, this.metadata.targetMethod, result);
        }

        protected Collection<? extends Cache> getCaches() {
            return this.caches;
        }

        protected Collection<String> getCacheNames() {
            return this.cacheNames;
        }

        private Collection<String> createCacheNames(Collection<? extends Cache> caches) {
            Collection<String> names = new ArrayList<>();
            for (Cache cache : caches) {
                names.add(cache.getName());
            }
            return names;
        }
    }

    @Data
    @AllArgsConstructor
    public static class FullKeyHolder {
        private Object keys;
        private String prefix;

        public static FullKeyHolder of(Object key) {
            return new FullKeyHolder(key, null);
        }

        public static FullKeyHolder of(Object key, String prefix) {
            return key instanceof Collection<?> keyList ?
                    new FullKeyHolder(keyList.stream().map(keyItem -> wrapKeyPrefix(prefix, keyItem)).collect(Collectors.toList()), prefix) :
                    new FullKeyHolder(prefix + ":" + key, prefix);
        }

        public static String removeKeyPrefix(@NonNull Object prefix, @NonNull String keyItem) {
            if (ObjectUtils.isEmpty(prefix)) {
                return keyItem;
            }
            Assert.notNull(keyItem, "keyItem must not be null");
            return prefix instanceof Optional<?> optional ? keyItem.replace(optional.get() + ":", "") : keyItem.replace(prefix + ":", "");
        }

        public static Object wrapKeyPrefix(@NonNull Object prefix, @NonNull Object keyItem) {
            if (ObjectUtils.isEmpty(prefix) || ObjectUtils.isEmpty(keyItem)) {
                return keyItem;
            }
            return prefix instanceof Optional<?> optional ? optional.get() + ":" + keyItem : prefix + ":" + keyItem;
        }
    }
}
