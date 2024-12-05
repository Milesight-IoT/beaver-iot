package com.milesight.beaveriot.rule.model.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * @author leon
 */
@Data
public class RuleNode  {

    private String id;

    @JsonIgnore
    private String componentId;

    @JsonIgnore
    private String nodeName;

    private JsonNode parameters;

}
