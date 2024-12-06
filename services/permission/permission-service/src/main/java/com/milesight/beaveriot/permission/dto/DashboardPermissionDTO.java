package com.milesight.beaveriot.permission.dto;

import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/26 11:08
 */
@Data
public class DashboardPermissionDTO {

    private boolean isHasAllPermission;
    private List<String> dashboardIds;

}
