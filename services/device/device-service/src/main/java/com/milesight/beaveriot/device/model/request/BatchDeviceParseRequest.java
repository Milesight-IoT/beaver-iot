package com.milesight.beaveriot.device.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * BatchDeviceParseRequest class.
 *
 * @author simon
 * @date 2025/7/4
 */
@Data
public class BatchDeviceParseRequest {
    @NotBlank
    private String integration;

    @NotNull
    private MultipartFile file;
}
