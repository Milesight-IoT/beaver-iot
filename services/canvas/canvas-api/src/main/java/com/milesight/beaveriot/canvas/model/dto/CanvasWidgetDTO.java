package com.milesight.beaveriot.canvas.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * CanvasWidgetDTO class.
 *
 * @author simon
 * @date 2025/9/10
 */
@Data
public class CanvasWidgetDTO implements Serializable {
    private String widgetId;
    private Map<String, Object> data;
}
