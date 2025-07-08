package com.milesight.beaveriot.device.model;

import lombok.Data;

import java.util.List;

/**
 * DeviceBatchError class.
 *
 * @author simon
 * @date 2025/7/8
 */
@Data
public class DeviceBatchError {
    @Data
    public static class ErrorDetail {
        private Integer id;
        private String msg;
    }

    private List<ErrorDetail> errors;
}
