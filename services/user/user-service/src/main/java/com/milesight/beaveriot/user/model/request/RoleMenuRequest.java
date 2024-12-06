package com.milesight.beaveriot.user.model.request;

import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/21 17:20
 */
@Data
public class RoleMenuRequest {

    private List<Long> menuIds;

}
