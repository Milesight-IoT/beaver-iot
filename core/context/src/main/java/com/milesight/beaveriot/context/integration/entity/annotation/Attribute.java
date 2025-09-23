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

    /**
     * 12,16
     */
    String lengthRange() default "";

    int fractionDigits() default AttributeBuilder.POSITIVE_INT_NAN;

    /**
     * <b>[component]:[arguments]</b><br/><br/>
     * For example:
     * <ul>
     *  <li>regex:([0-9A-Fa-f]{2}){16}</li>
     *  <li>hex:16</li>
     * </ul>
     */
    String format() default "";

    String defaultValue() default "";

    Class<? extends Enum>[] enumClass() default {};

    /**
     * Whether it is optional, default is false
     * @return
     */
    boolean optional() default false;

    int important() default AttributeBuilder.POSITIVE_INT_NAN;
}
