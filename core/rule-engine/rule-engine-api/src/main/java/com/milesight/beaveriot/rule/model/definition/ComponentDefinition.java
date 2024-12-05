package com.milesight.beaveriot.rule.model.definition;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author leon
 */
@Data
public class ComponentDefinition extends BaseDefinition {

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
    protected final Map<String, ComponentOptionDefinition> exchangeProperties = new LinkedHashMap<>();
    protected final Map<String, ComponentOptionDefinition> headers = new LinkedHashMap<>();
    protected final Map<String, ComponentOptionDefinition> properties = new LinkedHashMap<>();
    protected final Map<String, ComponentOptionDefinition> outputProperties = new LinkedHashMap<>();

}
