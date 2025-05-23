package com.milesight.beaveriot.dashboard.model.request;

import com.milesight.beaveriot.dashboard.constants.DashboardDataFieldConstants;
import com.milesight.beaveriot.dashboard.model.dto.DashboardWidgetDTO;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/17 10:24
 */
@Data
public class UpdateDashboardRequest {

    private String name;

    @Size(max = DashboardDataFieldConstants.WIDGET_MAX_COUNT_PER_DASHBOARD)
    private List<DashboardWidgetDTO> widgets;

}
