package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.user.enums.MenuType;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/22 8:59
 */
@Data
public class CreateMenuRequest {

    private String code;
    private String name;
    private MenuType type;
    private String parentId;

}
