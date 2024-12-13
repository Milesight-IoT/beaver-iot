package com.milesight.beaveriot.rule.model.definition;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class ComponentBaseDefinition extends BaseDefinition {

    protected String kind = "component";
    protected String description;
    protected String label;
    protected String javaType;
    protected String scheme = "bean";
    protected String extendsScheme;
    protected String syntax = "bean:beanName";
    protected boolean consumerOnly;
    protected boolean producerOnly;
    protected boolean remote;
    /**
     * extension property, whether the component is testable
     */
    protected boolean testable;
    /**
     * extension property, component type
     */
    protected String type;

}
