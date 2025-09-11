package com.milesight.beaveriot.dashboard.model.request;

import com.milesight.beaveriot.canvas.constants.CanvasDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DashboardCanvasCreateRequest class.
 *
 * @author simon
 * @date 2025/9/10
 */
@Data
public class DashboardCanvasCreateRequest {
    @Size(max = CanvasDataFieldConstants.CANVAS_NAME_MAX_LENGTH)
    @NotBlank
    private String name;
}
