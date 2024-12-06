package com.milesight.beaveriot.user.model.response;

import com.milesight.beaveriot.user.enums.MenuType;
import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/22 10:35
 */
@Data
public class MenuResponse {

    private String menuId;
    private String code;
    private String name;
    private MenuType type;
    private String parentId;
    private List<MenuResponse> children;

}
