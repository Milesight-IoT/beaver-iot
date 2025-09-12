package com.milesight.beaveriot.rule.manager.model.response;

import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data

@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WorkflowLogDetailResponse extends WorkflowLogResponse {
    private List<NodeTraceInfo> traceInfo;
}
