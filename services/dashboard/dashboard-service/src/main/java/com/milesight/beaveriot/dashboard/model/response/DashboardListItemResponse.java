package com.milesight.beaveriot.dashboard.model.response;

import com.milesight.beaveriot.dashboard.enums.DashboardCoverType;
import lombok.*;

/**
 * @author loong
 * @date 2024/10/18 9:45
 */
@Data
public class DashboardListItemResponse {

    private String dashboardId;
    private String userId;
    private String name;
    private Boolean home;
    private String description;
    private DashboardCoverType coverType;
    private String coverData;
    private String mainCanvasId;
    private String createdAt;

}
