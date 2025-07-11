package com.milesight.beaveriot.user.model.response;

import com.milesight.beaveriot.permission.enums.MenuType;
import lombok.*;

/**
 * @author loong
 * @date 2024/11/22 13:48
 */
@Data
public class UserMenuResponse {

    private String menuId;
    private String code;
    private String name;
    private MenuType type;
    private String parentId;

}
