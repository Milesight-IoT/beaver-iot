package com.milesight.beaveriot.rule.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author leon
 */
@Data
public class ComponentOptionDefinition {

    protected String name;
    protected int index;
    protected String kind;
    protected String displayName;
    protected String label;
    protected boolean required;
    protected String type;
    protected String javaType;
    @JsonProperty("enum")
    protected List<String> enums;

    protected boolean multiValue;
    protected String prefix;
    protected boolean secret;
    protected Object defaultValue;
    protected String defaultValueNote;
    protected String description;

    //extension ui parameter
    protected String uiComponent;
    protected String uiComponentTags;
    protected String uiComponentGroup;
}
