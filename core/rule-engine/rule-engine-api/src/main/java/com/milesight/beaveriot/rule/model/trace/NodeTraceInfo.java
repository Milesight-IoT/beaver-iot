package com.milesight.beaveriot.rule.model.trace;

import com.milesight.beaveriot.rule.enums.ExecutionStatus;
import lombok.Data;

/**
 * @author leon
 */
@Data
public class NodeTraceInfo {

    private String messageId;

    private String nodeId;

    private String nodeLabel;

    private ExecutionStatus status;

    private String errorMessage;

    private long startTime;

    private long timeCost;

    private String input;

    private String output;

    public void causeException(Exception ex) {
        this.status = ExecutionStatus.ERROR;
        this.errorMessage = ex.getMessage();
    }
}
