package com.milesight.beaveriot.entity.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.*;
import org.springframework.http.HttpStatus;

/**
 * @author loong
 * @date 2024/10/16 17:42
 */
@RequiredArgsConstructor
@Getter
public enum EntityErrorCode implements ErrorCodeSpec {

    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND.value()),
    ENTITY_TAG_NOT_FOUND(HttpStatus.NOT_FOUND.value()),
    ENTITY_TAG_NAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST.value()),
    NUMBER_OF_ENTITY_TAGS_EXCEEDED(HttpStatus.BAD_REQUEST.value()),
    NUMBER_OF_TAGS_PER_ENTITY_EXCEEDED(HttpStatus.BAD_REQUEST.value()),
    ;

    private final int status;
    private final String errorCode;
    private final String errorMessage;
    private final String detailMessage;

    EntityErrorCode() {
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    EntityErrorCode(int status) {
        this.status = status;
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    EntityErrorCode(String errorMessage) {
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = null;
    }

    EntityErrorCode(String errorMessage, String detailMessage) {
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = detailMessage;
    }

}
