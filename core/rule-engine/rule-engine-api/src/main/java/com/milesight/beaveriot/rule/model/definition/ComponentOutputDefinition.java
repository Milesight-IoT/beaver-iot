package com.milesight.beaveriot.rule.model.definition;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class ComponentOutputDefinition extends ComponentOptionExtensionDefinition {

    protected String name;
    protected int index;
    protected String displayName;
    protected String type;
    protected String javaType;
    protected String genericType;
    protected String description;
    protected boolean editable;

}
