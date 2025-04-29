package com.milesight.beaveriot.scheduler.integration;

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

    String name();
    String timeZone() default "";
    String timeZoneEntity() default "";
    String cron() default "";
    String cronEntity() default "";
    long fixedRate() default -1;
    String fixedRateEntity() default "";
    String timeUnitEntity() default "";
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    String enabledEntity() default "";
    boolean enabled() default true;
}
