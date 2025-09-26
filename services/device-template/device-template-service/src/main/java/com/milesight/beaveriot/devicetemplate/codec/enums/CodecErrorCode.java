package com.milesight.beaveriot.devicetemplate.codec.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

public enum CodecErrorCode implements ErrorCodeSpec {
    CODEC_DECODE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "codec_decode_failed", "Codec decode failed"),
    CODEC_ENCODE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "codec_encode_failed", "Codec encode failed"),
    CODEC_EXECUTOR_BUILD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), "codec_executor_build_failed", "Codec executor build failed");

    private final String errorCode;
    private String errorMessage;
    private String detailMessage;
    private int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

    CodecErrorCode(int status, String errorCode, String errorMessage, String detailMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
        this.detailMessage = detailMessage;
    }

    CodecErrorCode(int status, String errorCode) {
        this.errorCode = errorCode;
        this.status = status;
    }

    CodecErrorCode(int status, String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
    }

    CodecErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    CodecErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    public String formatMessage(Object... args) {
        return MessageFormat.format(errorMessage, args);
    }

    @Override
    public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
