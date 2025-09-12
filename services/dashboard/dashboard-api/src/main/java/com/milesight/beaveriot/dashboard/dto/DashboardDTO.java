package com.milesight.beaveriot.dashboard.dto;

import lombok.Data;

/**
 * @author loong
 * @date 2024/11/26 11:35
 */
@Data
public class DashboardDTO {

    private Long dashboardId;
    private String dashboardName;
    private Long mainCanvasId;
    private Long userId;
    private Long createdAt;

}
