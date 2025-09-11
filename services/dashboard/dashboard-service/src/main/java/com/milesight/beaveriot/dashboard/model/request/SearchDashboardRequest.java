package com.milesight.beaveriot.dashboard.model.request;

import com.milesight.beaveriot.dashboard.constants.DashboardDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SearchDashboardRequest class.
 *
 * @author simon
 * @date 2025/9/9
 */
@Data
public class SearchDashboardRequest {
    @Size(max = DashboardDataFieldConstants.DASHBOARD_NAME_MAX_LENGTH)
    private String name;
}
