package com.milesight.beaveriot.device.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * BatchDeviceTemplateRequest class.
 *
 * @author simon
 * @date 2025/6/26
 */
@Data
public class BatchDeviceTemplateRequest {
    @NotBlank
    private String integration;
}
