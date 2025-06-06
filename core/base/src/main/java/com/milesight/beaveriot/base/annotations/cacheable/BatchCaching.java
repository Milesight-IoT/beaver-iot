
package com.milesight.beaveriot.base.annotations.cacheable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;

/**
 * @author leon
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Reflective
public @interface BatchCaching {

	/**
	 * The cacheable operations to apply. only one of the cacheable
	 * @return
	 */
	BatchCacheable[] cacheable() default {};

	/**
	 * The cache put operations to apply. only one of the put
	 * @return
	 */
	BatchCachePut[] put() default {};

	/**
	 * The cache evict operations to apply. only one of the evict
	 * @return
	 */
	BatchCacheEvict[] evict() default {};

}
