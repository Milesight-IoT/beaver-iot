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

    private List<NodeTraceInfo> traceInfos = new ArrayList<>();

    public NodeTraceInfo findTraceInfo(String nodeId) {
        return traceInfos.stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    public NodeTraceInfo getLastNodeTrace() {
        return ObjectUtils.isEmpty(traceInfos) ? null : traceInfos.get(traceInfos.size() - 1);
    }
}
