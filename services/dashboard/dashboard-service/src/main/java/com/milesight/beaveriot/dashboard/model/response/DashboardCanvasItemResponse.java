package com.milesight.beaveriot.dashboard.model.response;

import com.milesight.beaveriot.canvas.model.dto.CanvasDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DashboardCanvasItemResponse class.
 *
 * @author simon
 * @date 2025/9/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DashboardCanvasItemResponse extends CanvasDTO {
    private String canvasId;
    private Boolean isMain;
}
