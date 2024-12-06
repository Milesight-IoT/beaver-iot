package com.milesight.beaveriot.user.model.response;

import lombok.Data;

/**
 * @author loong
 * @date 2024/11/20 10:22
 */
@Data
public class RoleResponse {

    private String roleId;
    private String name;
    private String description;
    private String createdAt;
    private Integer userRoleCount;
    private Integer roleIntegrationCount;

}
