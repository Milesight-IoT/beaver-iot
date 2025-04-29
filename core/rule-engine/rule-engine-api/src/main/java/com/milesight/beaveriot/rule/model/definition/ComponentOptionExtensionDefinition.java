package com.milesight.beaveriot.rule.model.definition;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class ComponentOptionExtensionDefinition {

    //extension ui parameter
    protected String uiComponent;
    protected String uiComponentTags;
    protected String uiComponentGroup;
    protected boolean loggable;
    protected String initialValue;
}
