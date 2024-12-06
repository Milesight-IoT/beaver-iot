package com.milesight.beaveriot.user.model.response;

import lombok.Data;

/**
 * @author loong
 * @date 2024/11/20 10:36
 */
@Data
public class UserRoleResponse {

    private String roleId;
    private String userId;
    private String userNickname;
    private String userEmail;

}
