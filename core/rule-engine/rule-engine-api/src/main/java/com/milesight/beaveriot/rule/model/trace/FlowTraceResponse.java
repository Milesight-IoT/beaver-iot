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
public class FlowTraceResponse {

    private ExecutionStatus status = ExecutionStatus.SUCCESS;

    private List<NodeTraceResponse> traceInfos = new ArrayList<>();

    public NodeTraceResponse findTraceInfo(String nodeId) {
        return traceInfos.stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    public NodeTraceResponse getLastNodeTrace() {
        return ObjectUtils.isEmpty(traceInfos) ? null : traceInfos.get(traceInfos.size() - 1);
    }
}
