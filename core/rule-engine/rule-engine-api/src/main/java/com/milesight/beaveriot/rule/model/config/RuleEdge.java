package com.milesight.beaveriot.rule.model.config;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class RuleEdge {

    private String id;

    private String source;

    private String target;

    private String type;

//    private String sourceHandle;

}
