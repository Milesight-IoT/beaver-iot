package com.milesight.beaveriot.dashboard.model.request;

import com.milesight.beaveriot.dashboard.model.dto.DashboardWidgetDTO;
import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/17 10:24
 */
@Data
public class UpdateDashboardRequest {

    private String name;
    private List<DashboardWidgetDTO> widgets;

}
