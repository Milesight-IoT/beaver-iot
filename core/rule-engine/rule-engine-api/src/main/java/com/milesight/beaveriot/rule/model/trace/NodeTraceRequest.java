package com.milesight.beaveriot.rule.model.trace;

import com.milesight.beaveriot.rule.model.config.RuleNode;
import lombok.Data;

/**
 * @author leon
 */
@Data
public class NodeTraceRequest {

    private RuleNode nodeConfig;

    private Object input;

}
