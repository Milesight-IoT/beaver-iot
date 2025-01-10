package com.milesight.beaveriot.eventbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author leon
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventSubscribe {

    String[] eventType() default {};

    String payloadKeyExpression() ;

}
