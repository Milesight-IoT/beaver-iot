package com.milesight.beaveriot.rule.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * describe the uri parameter extension of the rule component.
 *
 * @author leon
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UriParamExtension {

    /**
     * describe the ui component of front.
     *
     * @return
     */
    String uiComponent() default "";

    /**
     * describe the ui component tags.
     *
     * @return
     */
    String uiComponentTags() default "";

    /**
     * describe the ui component group.
     *
     * @return
     */
    String uiComponentGroup() default "";

}
