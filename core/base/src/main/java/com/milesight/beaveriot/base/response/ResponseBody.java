package com.milesight.beaveriot.base.response;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @param <T>
 * @author leon
 */
@Getter
@Slf4j
public class ResponseBody<T> {

    /**
     * Response Data
     */
    private T data;

    /**
     * response status
     */
    private String status;

    /**
     * request id
     */
    private String requestId;

    /**
     * Error code (returned only when the response status is Failed)
     */
    private String errorCode;

    /**
     * Error message (returned only when the response status is Failed)
     */
    private String errorMessage;

    /**
     * Detailed error description information
     */
    private String detailMessage;

    public ResponseBody<T> data(T data) {
        this.data = data;
        return this;
    }

    public ResponseBody<T> onSuccess() {
        this.status = ResponseBuilder.DEFAULT_RESPONSE_STATUS_SUCCESS;
        return this;
    }

    public ResponseBody<T> onFailed() {
        this.status = ResponseBuilder.DEFAULT_RESPONSE_STATUS_FAILED;
        return this;
    }

    public ResponseBody<T> requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public ResponseBody<T> errorCode(String errCode) {
        this.errorCode = errCode;
        return this;
    }

    public ResponseBody<T> errorMessage(String errMsg) {
        this.errorMessage = errMsg;
        return this;
    }

    public ResponseBody<T> detailMessage(String detailMsg) {
        this.detailMessage = detailMsg;
        return this;
    }

}