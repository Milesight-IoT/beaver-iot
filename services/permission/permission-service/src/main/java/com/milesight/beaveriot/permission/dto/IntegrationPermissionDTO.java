package com.milesight.beaveriot.permission.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/28 17:13
 */
@Data
public class IntegrationPermissionDTO implements Serializable {

    private boolean isHasAllPermission;
    private List<String> integrationIds;

}
