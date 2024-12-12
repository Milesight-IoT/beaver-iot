package com.milesight.beaveriot.user.model.request;

import lombok.Data;

/**
 * @author loong
 * @date 2024/11/20 8:58
 */
@Data
public class CreateRoleRequest {

    private String name;
    private String description;

}
