package com.milesight.beaveriot.rule.model.definition;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class ComponentOutputDefinition {

    protected String name;
    protected int index;
    protected String displayName;
    protected String type;
    protected String javaType;
    protected String description;
    protected ComponentOptionDefinition inputDefinition;

}
