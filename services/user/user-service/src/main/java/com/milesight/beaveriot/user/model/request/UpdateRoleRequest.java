package com.milesight.beaveriot.user.model.request;

import lombok.Data;

/**
 * @author loong
 * @date 2024/11/20 10:10
 */
@Data
public class UpdateRoleRequest {

    private String name;
    private String description;

}
