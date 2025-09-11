package com.milesight.beaveriot.dashboard.model.request;

import com.milesight.beaveriot.dashboard.constants.DashboardDataFieldConstants;
import com.milesight.beaveriot.dashboard.enums.DashboardCoverType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author simon
 * @date 2025/9/8
 */
@Data
public class DashboardInfoRequest {
    @Size(max = DashboardDataFieldConstants.DASHBOARD_NAME_MAX_LENGTH)
    @NotBlank
    private String name;

    @Size(max = DashboardDataFieldConstants.DASHBOARD_DESCRIPTION_MAX_LENGTH)
    private String description;

    private DashboardCoverType coverType;

    private String coverData;

}
