package com.milesight.beaveriot.base.error;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/7/14 15:35
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ErrorHolderExt extends ErrorHolder {
    private Map<String, Object> extraData;

    protected ErrorHolderExt(String errorCode, String errorMessage, Map<String, Object> extraData) {
        super(errorCode, errorMessage);
        this.extraData = extraData;
    }

    public static ErrorHolderExt of(String errorCode, String errorMessage) {
        return of(errorCode, errorMessage, null);
    }

    public static ErrorHolderExt of(String errorCode, String errorMessage, Map<String, Object> extraData) {
        return new ErrorHolderExt(errorCode, errorMessage, extraData);
    }
}