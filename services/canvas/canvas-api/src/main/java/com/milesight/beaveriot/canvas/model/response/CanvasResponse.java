package com.milesight.beaveriot.canvas.model.response;

import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.model.dto.CanvasWidgetDTO;
import com.milesight.beaveriot.device.dto.DeviceResponseData;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import lombok.Data;

import java.util.List;

/**
 * CanvasResponse class.
 *
 * @author simon
 * @date 2025/9/9
 */
@Data
public class CanvasResponse {
    private String id;
    private String name;
    private CanvasAttachType attachType;
    private String attachId;
    private List<CanvasWidgetDTO> widgets;
    private List<String> entityIds;
    private List<EntityResponse> entities;
    private List<String> deviceIds;
    private List<DeviceResponseData> devices;
}
