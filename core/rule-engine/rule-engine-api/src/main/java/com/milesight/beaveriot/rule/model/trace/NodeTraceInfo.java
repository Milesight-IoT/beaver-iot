package com.milesight.beaveriot.rule.model.trace;

import com.milesight.beaveriot.rule.enums.ExecutionStatus;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * @author leon
 */
@Data
public class NodeTraceInfo {

    private String messageId;

    private String nodeId;

    private String nodeLabel;

    private String nodeName;

    private ExecutionStatus status = ExecutionStatus.SUCCESS;

    private String errorMessage;

    private long startTime;

    private long timeCost;

    private String input;

    private String output;

    private String parentTraceId;

    public void causeException(Exception ex) {
        this.status = ExecutionStatus.ERROR;
        this.errorMessage = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.toString();
    }
}
