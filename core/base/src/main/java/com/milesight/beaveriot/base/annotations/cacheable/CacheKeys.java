package com.milesight.beaveriot.base.annotations.cacheable;

import java.lang.annotation.*;

/**
 * @author leon
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheKeys {
}
