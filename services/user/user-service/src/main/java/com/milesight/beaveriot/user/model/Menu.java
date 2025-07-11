package com.milesight.beaveriot.user.model;

import com.milesight.beaveriot.permission.enums.MenuType;
import lombok.*;
import lombok.experimental.*;

/**
 * @author loong
 * @date 2024/11/21 16:08
 */
@Data
@FieldNameConstants
public class Menu {
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private MenuType type;
}
