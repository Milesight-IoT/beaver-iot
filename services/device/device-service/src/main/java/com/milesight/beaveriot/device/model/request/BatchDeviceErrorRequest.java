package com.milesight.beaveriot.device.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * BatchDeviceErrorRequest class.
 *
 * @author simon
 * @date 2025/7/8
 */
@Data
public class BatchDeviceErrorRequest {
    @NotBlank
    private String integration;

    @NotNull
    private MultipartFile file;

    @NotBlank
    private String errors;
}
