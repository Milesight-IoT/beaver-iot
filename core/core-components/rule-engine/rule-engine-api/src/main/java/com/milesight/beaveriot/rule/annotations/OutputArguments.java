package com.milesight.beaveriot.rule.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * describe the output arguments of the rule.
 *
 * @author leon
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OutputArguments {

    /**
     * The name of the parameter.
     *
     * @return
     */
    String name() default "";

    /**
     * A human display name of the parameter.
     * <p/>
     * This is used for documentation and tooling only.
     */
    String displayName() default "";

    String description() default "";

    /**
     * the java type of the parameter, see {@link org.apache.camel.spi.UriParam#javaType()}
     *
     * @return
     */
    String javaType() default "";
}
