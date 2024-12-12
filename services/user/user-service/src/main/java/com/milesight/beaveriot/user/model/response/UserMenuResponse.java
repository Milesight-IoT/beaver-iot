package com.milesight.beaveriot.user.model.response;

import com.milesight.beaveriot.user.enums.MenuType;
import lombok.Data;

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
