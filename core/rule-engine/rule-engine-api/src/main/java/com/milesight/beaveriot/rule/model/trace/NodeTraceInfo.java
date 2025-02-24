package com.milesight.beaveriot.rule.model.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        this.errorMessage = (ex.getCause() != null) ? getExchangeMessage(ex.getCause()) : getExchangeMessage(ex);
    }

    private String getExchangeMessage(Throwable ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.toString();
    }
}
