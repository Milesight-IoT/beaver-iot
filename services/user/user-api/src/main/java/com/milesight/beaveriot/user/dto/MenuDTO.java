package com.milesight.beaveriot.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author loong
 * @date 2024/12/4 13:14
 */
@Data
public class MenuDTO implements Serializable {

    private Long menuId;
    private String menuCode;

}
