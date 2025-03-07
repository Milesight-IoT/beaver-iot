package com.milesight.beaveriot.rule.model.trace;

import com.milesight.beaveriot.rule.enums.ExecutionStatus;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
@Data
public class FlowTraceInfo {

    private ExecutionStatus status = ExecutionStatus.SUCCESS;

    private String flowId;

    private long startTime = System.currentTimeMillis();

    private long timeCost;

    private String message;

    private List<NodeTraceInfo> traceInfos = new ArrayList<>();

    public boolean isEmpty() {
        return ObjectUtils.isEmpty(traceInfos);
    }

    public static FlowTraceInfo create(String flowId) {
        FlowTraceInfo flowTraceInfo = new FlowTraceInfo();
        flowTraceInfo.setFlowId(flowId);
        return flowTraceInfo;
    }

    public NodeTraceInfo findTraceInfo(String nodeId, String messageId) {
        return traceInfos.stream()
                .filter(node -> node.getNodeId().equals(nodeId) && messageId.equals(node.getMessageId()))
                .findFirst()
                .orElse(null);
    }

    public NodeTraceInfo findLastNodeTrace() {
        return ObjectUtils.isEmpty(traceInfos) ? null : traceInfos.get(traceInfos.size() - 1);
    }
}
