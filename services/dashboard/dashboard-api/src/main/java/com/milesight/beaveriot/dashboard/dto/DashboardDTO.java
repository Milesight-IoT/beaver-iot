package com.milesight.beaveriot.dashboard.dto;

import com.milesight.beaveriot.eventbus.api.IdentityKey;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/26 11:35
 */
@Data
public class DashboardDTO implements IdentityKey {

    private Long dashboardId;
    private String dashboardName;
    private Long mainCanvasId;
    private Long userId;
    private Long createdAt;

    @Override
    public String getKey() {
        return this.dashboardId.toString();
    }
}
