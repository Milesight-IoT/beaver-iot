package com.milesight.beaveriot.canvas.model.request;

import com.milesight.beaveriot.canvas.model.dto.CanvasWidgetDTO;
import com.milesight.beaveriot.canvas.constants.CanvasDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * CanvasUpdateRequest class.
 *
 * @author simon
 * @date 2025/9/10
 */
@Data
public class CanvasUpdateRequest {
    @Size(max = CanvasDataFieldConstants.CANVAS_NAME_MAX_LENGTH)
    @NotBlank
    private String name;

    @Size(max = CanvasDataFieldConstants.WIDGET_MAX_COUNT_PER_DASHBOARD)
    private List<CanvasWidgetDTO> widgets;

    @Size(max = CanvasDataFieldConstants.ENTITY_MAX_COUNT_PER_DASHBOARD)
    private List<Long> entityIds;
}
