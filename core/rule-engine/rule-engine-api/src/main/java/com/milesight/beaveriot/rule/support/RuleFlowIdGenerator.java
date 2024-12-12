package com.milesight.beaveriot.rule.support;

import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.UUID;

/**
 * @author leon
 */
public class RuleFlowIdGenerator {

    public static final String PARALLEL_NODE_ID_PREFIX = "PARALLEL_";
    public static final String BRANCH_NODE_ID_PREFIX = "BRANCH_";
    public static final String INNER_NODE_ID_TEMPLATE = "inner.{0}.{1}";
    public static final String FLOW_ID_PREFIX = "flow.";
    public static final String FLOW_NODE_ID_TEMPLATE = FLOW_ID_PREFIX + "{0}.{1}";

    private RuleFlowIdGenerator() {
    }

    public static String generateNamespacedId(String flowId, String nodeId) {
        if (ObjectUtils.isEmpty(nodeId)) {
            return null;
        }
        return MessageFormat.format(FLOW_NODE_ID_TEMPLATE, flowId, nodeId);
    }

    public static String removeNamespacedId(String flowId, String nodeId) {
        return nodeId.replace(FLOW_ID_PREFIX + flowId + ".", "");
    }

    public static String generateRandomId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "");
    }

    public static String generateNamespacedParallelId(String flowId, int order) {
        return MessageFormat.format(INNER_NODE_ID_TEMPLATE, flowId, PARALLEL_NODE_ID_PREFIX + order);
    }

    public static String generateNamespacedBranchId(String flowId, int order) {
        return MessageFormat.format(INNER_NODE_ID_TEMPLATE, flowId, BRANCH_NODE_ID_PREFIX + order);
    }
}
