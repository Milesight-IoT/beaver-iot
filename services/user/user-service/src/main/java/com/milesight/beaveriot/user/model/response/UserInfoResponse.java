package com.milesight.beaveriot.user.model.response;

import com.milesight.beaveriot.user.enums.MenuType;
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
    private List<Menu> menus;

    @Data
    public static class Role {
        private String roleId;
        private String roleName;
    }

    @Data
    public static class Menu {
        private String menuId;
        private String code;
        private String name;
        private MenuType type;
        private String parentId;
    }

}
