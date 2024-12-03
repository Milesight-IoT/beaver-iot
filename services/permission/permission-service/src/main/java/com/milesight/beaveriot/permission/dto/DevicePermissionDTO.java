package com.milesight.beaveriot.permission.dto;

import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/28 17:09
 */
@Data
public class DevicePermissionDTO {

    private boolean isHasAllPermission;
    private List<String> deviceIds;

}
