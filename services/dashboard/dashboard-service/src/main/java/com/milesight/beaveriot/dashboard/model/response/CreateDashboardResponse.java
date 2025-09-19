package com.milesight.beaveriot.dashboard.model.response;

/**
 * @author loong
 * @date 2024/10/28 12:50
 */

import lombok.Data;

@Data
public class CreateDashboardResponse {

    private String dashboardId;

    private String mainCanvasId;

}
