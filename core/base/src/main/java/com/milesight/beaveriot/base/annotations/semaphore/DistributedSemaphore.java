package com.milesight.beaveriot.base.annotations.semaphore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * author: Luxb
 * create: 2025/7/28 15:13
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedSemaphore {
    String name() default "";
    int permits() default 1;
    long timeout() default 0;
    SemaphoreScope scope() default SemaphoreScope.TENANT;
}