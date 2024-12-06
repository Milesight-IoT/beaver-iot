package com.milesight.beaveriot.user.model.request;

import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/20 10:45
 */
@Data
public class UserRoleRequest {

    private List<Long> userIds;

}
