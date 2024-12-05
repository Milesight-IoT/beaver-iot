package com.milesight.beaveriot.rule.model.trace;

import com.milesight.beaveriot.rule.enums.ExecutionStatus;
import lombok.Data;

/**
 * @author leon
 */
@Data
public class NodeTraceResponse {

    private String messageId;

    private String nodeId;

    private String nodeName;

    private ExecutionStatus status;

    private String errorMessage;

    private long startTime;

    private long cost;

    private Object input;

    private Object output;

    public void causeException(Exception ex) {
        this.status = ExecutionStatus.ERROR;
        this.errorMessage = ex.getMessage();
    }
}
