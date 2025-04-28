package com.milesight.beaveriot.permission.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/26 11:08
 */
@Data
public class DashboardPermissionDTO implements Serializable {

    private boolean isHasAllPermission;
    private List<String> dashboardIds;

}
