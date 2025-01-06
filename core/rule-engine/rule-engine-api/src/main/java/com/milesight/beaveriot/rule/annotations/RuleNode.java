package com.milesight.beaveriot.rule.annotations;

import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * describe the rule component definition.
 * extension of {@link org.apache.camel.spi.UriEndpoint}
 *
 * @author leon
 */
@Scope("prototype")
@Component
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RuleNode {

    /**
     * The name of the component.
     * when schema is bean, the value is the bean name.
     *
     * @return
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * The name of the component. see {@link com.milesight.beaveriot.rule.constants.RuleNodeType}
     *
     * @return
     */
    String type() default "";

    /**
     * describe the headers of the component, priority is higher than UriEndpoint headersClass.
     * see {@link org.apache.camel.spi.UriEndpoint}
     *
     * @return
     */
    Class<?> headersClass() default void.class;

    /**
     * describe the properties of the component
     *
     * @return
     */
    Class<?> propertiesClass() default void.class;

    String description() default "";

    /**
     * describe the title of the component.
     *
     * @return
     */
    String title() default "";

    /**
     * if the component is testable, default is true.
     *
     * @return
     */
    boolean testable() default true;
}
