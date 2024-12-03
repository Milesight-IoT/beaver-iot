package com.milesight.beaveriot.user.model.response;

import com.milesight.beaveriot.user.enums.ResourceType;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/22 13:23
 */
@Data
public class RoleResourceResponse {

    private String resourceId;
    private ResourceType resourceType;

}
