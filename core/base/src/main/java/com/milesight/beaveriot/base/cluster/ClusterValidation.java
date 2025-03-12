package com.milesight.beaveriot.base.cluster;

import java.lang.annotation.*;

/**
 * @author leon
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ClusterValidation {

    /**
     * Required beans.
     * @return the class [ ]
     */
    Class<?>[] requiredBeans() default {};

    /**
     * Unsupported beans.
     * @return
     */
    Class<?>[] unsupportedBeans() default {};
}
