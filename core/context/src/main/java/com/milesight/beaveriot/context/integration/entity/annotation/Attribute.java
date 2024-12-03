package com.milesight.beaveriot.context.integration.entity.annotation;

import com.milesight.beaveriot.context.integration.model.AttributeBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author leon
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute {

    String unit() default "";

    double max() default Double.NaN;

    double min() default Double.NaN;

    int maxLength() default AttributeBuilder.POSITIVE_INT_NAN;

    int minLength() default AttributeBuilder.POSITIVE_INT_NAN;

    int fractionDigits() default AttributeBuilder.POSITIVE_INT_NAN;

    String format() default "";

    Class<? extends Enum>[] enumClass() default {};
}
