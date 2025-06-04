package com.milesight.beaveriot.dashboard.model.response;

import com.milesight.beaveriot.dashboard.model.dto.DashboardWidgetDTO;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import lombok.*;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/18 9:45
 */
@Data
public class DashboardResponse {

    private String dashboardId;
    private String userId;
    private String name;
    private Boolean home;
    private String createdAt;
    private List<DashboardWidgetDTO> widgets;
    private List<String> entityIds;
    private List<EntityResponse> entities;

}
