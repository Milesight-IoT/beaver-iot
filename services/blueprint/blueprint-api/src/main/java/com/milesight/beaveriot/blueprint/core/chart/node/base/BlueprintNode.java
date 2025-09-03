package com.milesight.beaveriot.blueprint.core.chart.node.base;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintNodeShortTypeIdResolver;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "@t")
@JsonTypeIdResolver(BlueprintNodeShortTypeIdResolver.class)
public interface BlueprintNode {

    @JsonProperty("@n")
    String getBlueprintNodeName();

    @JsonProperty("@n")
    void setBlueprintNodeName(String blueprintNodeName);

    @JsonBackReference
    BlueprintNode getBlueprintNodeParent();

    @JsonBackReference
    void setBlueprintNodeParent(BlueprintNode blueprintNodeParent);

    @JsonProperty("@c")
    @JsonManagedReference
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<BlueprintNode> getBlueprintNodeChildren();

    @JsonProperty("@c")
    @JsonManagedReference
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    void setBlueprintNodeChildren(List<BlueprintNode> blueprintNodeChildren);

    @JsonProperty("@s")
    BlueprintNodeStatus getBlueprintNodeStatus();

    @JsonProperty("@s")
    void setBlueprintNodeStatus(BlueprintNodeStatus blueprintNodeStatus);

    void addChildNode(BlueprintNode childNode);

    interface Parser<T extends BlueprintNode> {

        T parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode, BlueprintParseContext context);

    }

    @FunctionalInterface
    interface ProcessingTask {

        void process();

    }

}
