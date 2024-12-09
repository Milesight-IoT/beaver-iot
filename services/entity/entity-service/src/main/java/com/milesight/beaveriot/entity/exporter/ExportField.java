package com.milesight.beaveriot.entity.exporter;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ExportField {

    @AliasFor("header")
    String value() default "";

    @AliasFor("value")
    String header() default "";

}
