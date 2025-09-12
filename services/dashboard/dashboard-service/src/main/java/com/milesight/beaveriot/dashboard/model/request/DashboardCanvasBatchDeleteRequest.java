package com.milesight.beaveriot.dashboard.model.request;

import lombok.Data;

import java.util.List;

/**
 * DashboardCanvasBatchDeleteRequest class.
 *
 * @author simon
 * @date 2025/9/10
 */
@Data
public class DashboardCanvasBatchDeleteRequest {
    private List<Long> canvasIds;
}
