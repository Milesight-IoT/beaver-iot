package com.milesight.beaveriot.context.i18n.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * author: Luxb
 * create: 2025/8/1 16:38
 **/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutowiredMessageResolver {
    /**
     * Model name
     */
    String value() default "";
}