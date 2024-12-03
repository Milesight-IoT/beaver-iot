package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.user.enums.MenuType;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/22 9:59
 */
@Data
public class UpdateMenuRequest {

    private String name;
    private MenuType type;

}
