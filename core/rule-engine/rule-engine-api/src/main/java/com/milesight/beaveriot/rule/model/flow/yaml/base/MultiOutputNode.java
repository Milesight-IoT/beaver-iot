package com.milesight.beaveriot.rule.model.flow.yaml.base;

import java.util.List;

/**
 * @author leon
 */
public interface MultiOutputNode extends OutputNode {

    List<OutputNode> getOutputNodes();
}
