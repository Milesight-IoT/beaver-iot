package org.springframework.cache.interceptor;

import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCachePut;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCaching;
import lombok.*;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.interceptor.operation.BatchCacheEvictOperation;
import org.springframework.cache.interceptor.operation.BatchCachePutOperation;
import org.springframework.cache.interceptor.operation.BatchCacheableOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
/**
 * @author leon
 */
public class SpringBatchCacheAnnotationParser implements CacheAnnotationParser, Serializable {

	private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS =
			Set.of(BatchCacheable.class, BatchCacheEvict.class, BatchCachePut.class, BatchCaching.class);


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		return AnnotationUtils.isCandidateClass(targetClass, CACHE_OPERATION_ANNOTATIONS);
	}

	@Override
	@Nullable
	public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
		DefaultCacheConfig defaultConfig = new DefaultCacheConfig(type);
		return parseCacheAnnotations(defaultConfig, type);
	}

	@Override
	@Nullable
	public Collection<CacheOperation> parseCacheAnnotations(Method method) {
		DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
		return parseCacheAnnotations(defaultConfig, method);
	}

	@Nullable
	private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
		Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, ae, false);
		if (ops != null && ops.size() > 1) {
			// More than one operation found -> local declarations override interface-declared ones...
			Collection<CacheOperation> localOps = parseCacheAnnotations(cachingConfig, ae, true);
			if (localOps != null) {
				return localOps;
			}
		}
		return ops;
	}

	@Nullable
	private Collection<CacheOperation> parseCacheAnnotations(
			DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {

		Collection<? extends Annotation> annotations = (localOnly ?
				AnnotatedElementUtils.getAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS) :
				AnnotatedElementUtils.findAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS));
		if (annotations.isEmpty()) {
			return null;
		}

		Integer cacheKeysPosition = ae instanceof Method method
				? CacheMissingHandler.retrieveCacheKeysAnnotationIndex(method)
				: null;

		Collection<CacheOperation> ops = new ArrayList<>(1);
		annotations.stream().filter(BatchCacheable.class::isInstance).map(BatchCacheable.class::cast).forEach(
				cacheable -> ops.add(parseCacheableAnnotation(ae, cachingConfig, cacheable, cacheKeysPosition)));
		annotations.stream().filter(BatchCacheEvict.class::isInstance).map(BatchCacheEvict.class::cast).forEach(
				cacheEvict -> ops.add(parseEvictAnnotation(ae, cachingConfig, cacheEvict, cacheKeysPosition)));
		annotations.stream().filter(BatchCachePut.class::isInstance).map(BatchCachePut.class::cast).forEach(
				cachePut -> ops.add(parsePutAnnotation(ae, cachingConfig, cachePut, cacheKeysPosition)));
		annotations.stream().filter(BatchCaching.class::isInstance).map(BatchCaching.class::cast).forEach(
				caching -> parseCachingAnnotation(ae, cachingConfig, caching, ops, cacheKeysPosition));
		return ops;
	}

	private BatchCacheableOperation parseCacheableAnnotation(
			AnnotatedElement ae, DefaultCacheConfig defaultConfig, BatchCacheable cacheable, @Nullable Integer cacheKeysPosition) {

		BatchCacheableOperation.Builder builder = new BatchCacheableOperation.Builder();

		//noinspection DuplicatedCode
		builder.setName(ae.toString());
		builder.setCacheNames(cacheable.cacheNames());
		builder.setCondition(cacheable.condition());
		builder.setKey(getOrAutoGenerateKey(cacheable.key(), cacheable.keyGenerator(), cacheKeysPosition));
		builder.setKeyGenerator(cacheable.keyGenerator());
		builder.setCacheManager(cacheable.cacheManager());
		builder.setCacheResolver(cacheable.cacheResolver());
		builder.setUnless(cacheable.unless());
		builder.setSync(cacheable.sync());
		builder.setKeyPrefix(cacheable.keyPrefix());

		defaultConfig.applyDefault(builder);
		BatchCacheableOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	private BatchCacheEvictOperation parseEvictAnnotation(
			AnnotatedElement ae, DefaultCacheConfig defaultConfig, BatchCacheEvict cacheEvict, @Nullable Integer cacheKeysPosition) {

		BatchCacheEvictOperation.Builder builder = new BatchCacheEvictOperation.Builder();

		//noinspection DuplicatedCode
		builder.setName(ae.toString());
		builder.setCacheNames(cacheEvict.cacheNames());
		builder.setCondition(cacheEvict.condition());
		builder.setKey(getOrAutoGenerateKey(cacheEvict.key(), cacheEvict.keyGenerator(), cacheKeysPosition));
		builder.setKeyGenerator(cacheEvict.keyGenerator());
		builder.setCacheManager(cacheEvict.cacheManager());
		builder.setCacheResolver(cacheEvict.cacheResolver());
		builder.setCacheWide(cacheEvict.allEntries());
		builder.setBeforeInvocation(cacheEvict.beforeInvocation());
		builder.setKeyPrefix(cacheEvict.keyPrefix());

		defaultConfig.applyDefault(builder);
		BatchCacheEvictOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	@NonNull
	private static String getOrAutoGenerateKey(String key, String keyGenerator, @Nullable Integer cacheKeysPosition) {
		if (!StringUtils.hasText(key) && !StringUtils.hasText(keyGenerator) && cacheKeysPosition != null) {
			return "#p" + cacheKeysPosition;
		}
		return key;
	}

	private CacheOperation parsePutAnnotation(
			AnnotatedElement ae, DefaultCacheConfig defaultConfig, BatchCachePut cachePut, @Nullable Integer cacheKeysPosition) {

		BatchCachePutOperation.Builder builder = new BatchCachePutOperation.Builder();

		//noinspection DuplicatedCode
		builder.setName(ae.toString());
		builder.setCacheNames(cachePut.cacheNames());
		builder.setCondition(cachePut.condition());
		builder.setKey(getOrAutoGenerateKey(cachePut.key(), cachePut.keyGenerator(), cacheKeysPosition));
		builder.setKeyGenerator(cachePut.keyGenerator());
		builder.setCacheManager(cachePut.cacheManager());
		builder.setCacheResolver(cachePut.cacheResolver());
		builder.setUnless(cachePut.unless());
		builder.setKeyPrefix(cachePut.keyPrefix());

		defaultConfig.applyDefault(builder);
		BatchCachePutOperation op = builder.build();
		validateCacheOperation(ae, op);

		return op;
	}

	private void parseCachingAnnotation(
			AnnotatedElement ae, DefaultCacheConfig defaultConfig, BatchCaching caching, Collection<CacheOperation> ops, @Nullable Integer cacheKeysPosition) {

		BatchCacheable[] cacheables = caching.cacheable();
		Assert.isTrue(cacheables.length <= 1, "@BatchCaching can only contain at most one @BatchCacheable annotation");

		for (BatchCacheable cacheable : cacheables) {
			ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable, cacheKeysPosition));
		}

		BatchCacheEvict[] cacheEvicts = caching.evict();
		Assert.isTrue(cacheEvicts.length <= 1, "@BatchCaching can only contain at most one @BatchCacheEvict annotation");
		for (BatchCacheEvict cacheEvict : cacheEvicts) {
			ops.add(parseEvictAnnotation(ae, defaultConfig, cacheEvict, cacheKeysPosition));
		}

		BatchCachePut[] cachePuts = caching.put();
		Assert.isTrue(cachePuts.length <= 1, "@BatchCaching can only contain at most one @BatchCachePut annotation");
		for (BatchCachePut cachePut : cachePuts) {
			ops.add(parsePutAnnotation(ae, defaultConfig, cachePut, cacheKeysPosition));
		}
	}

	/**
	 * Validates the specified {@link CacheOperation}.
	 * <p>Throws an {@link IllegalStateException} if the state of the operation is
	 * invalid. As there might be multiple sources for default values, this ensures
	 * that the operation is in a proper state before being returned.
	 * @param ae the annotated element of the cache operation
	 * @param operation the {@link CacheOperation} to validate
	 */
	private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
		if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
					"These attributes are mutually exclusive: either set the SpEL expression used to" +
					"compute the key at runtime or set the name of the KeyGenerator bean to use.");
		}
		if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
					"These attributes are mutually exclusive: the cache manager is used to configure a" +
					"default cache resolver if none is set. If a cache resolver is set, the cache manager" +
					"won't be used.");
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (other instanceof SpringBatchCacheAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringBatchCacheAnnotationParser.class.hashCode();
	}


	/**
	 * Provides default settings for a given set of cache operations.
	 */
	private static class DefaultCacheConfig {

		private final Class<?> target;

		@Nullable
		private String[] cacheNames;

		@Nullable
		private String keyGenerator;

		@Nullable
		private String cacheManager;

		@Nullable
		private String cacheResolver;

		private boolean initialized = false;

		public DefaultCacheConfig(Class<?> target) {
			this.target = target;
		}

		/**
		 * Apply the defaults to the specified {@link CacheOperation.Builder}.
		 * @param builder the operation builder to update
		 */
		public void applyDefault(CacheOperation.Builder builder) {
			if (!this.initialized) {
				CacheConfig annotation = AnnotatedElementUtils.findMergedAnnotation(this.target, CacheConfig.class);
				if (annotation != null) {
					this.cacheNames = annotation.cacheNames();
					this.keyGenerator = annotation.keyGenerator();
					this.cacheManager = annotation.cacheManager();
					this.cacheResolver = annotation.cacheResolver();
				}
				this.initialized = true;
			}

			if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
				builder.setCacheNames(this.cacheNames);
			}
			if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) &&
					StringUtils.hasText(this.keyGenerator)) {
				builder.setKeyGenerator(this.keyGenerator);
			}

			if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
				// One of these is set so we should not inherit anything
			}
			else if (StringUtils.hasText(this.cacheResolver)) {
				builder.setCacheResolver(this.cacheResolver);
			}
			else if (StringUtils.hasText(this.cacheManager)) {
				builder.setCacheManager(this.cacheManager);
			}
		}
	}

}
