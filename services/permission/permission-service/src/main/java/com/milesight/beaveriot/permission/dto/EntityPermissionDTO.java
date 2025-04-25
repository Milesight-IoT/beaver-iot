package com.milesight.beaveriot.permission.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/22 9:18
 */
@Data
public class EntityPermissionDTO implements Serializable {

    private boolean isHasAllPermission;
    private List<String> entityIds;

}
