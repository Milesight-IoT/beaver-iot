package com.milesight.beaveriot.scheduler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author loong
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Inherited
public @interface IntegrationScheduled {

    String scheduler() default "";
    String cron() default "";
    String cronEntity() default "";
    long fixedDelay() default -1;
    String fixedDelayEntity() default "";
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
    String timeUnitEntity() default "";
    boolean enabled() default true;
}
