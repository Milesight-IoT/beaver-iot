package com.milesight.beaveriot.permission.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/28 17:13
 */
@Data
public class PermissionDTO implements Serializable {
    private boolean haveAllPermissions;
    private List<String> ids;
}
