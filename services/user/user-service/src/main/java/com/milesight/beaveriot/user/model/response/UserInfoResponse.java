package com.milesight.beaveriot.user.model.response;

import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/21 17:04
 */
@Data
public class UserInfoResponse {

    private String userId;
    private String nickname;
    private String email;
    private List<Role> roles;
    private Boolean isSuperAdmin;
    private String createdAt;
    private List<MenuResponse> menus;

    @Data
    public static class Role {
        private String roleId;
        private String roleName;
    }

}
