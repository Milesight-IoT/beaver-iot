package com.milesight.beaveriot.dashboard.model.response;

import com.milesight.beaveriot.dashboard.model.dto.DashboardWidgetDTO;
import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/18 9:45
 */
@Data
public class DashboardResponse {

    private String dashboardId;
    private String name;
    private List<DashboardWidgetDTO> widgets;
    private String createdAt;

}
