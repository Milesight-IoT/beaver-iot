package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.user.enums.ResourceType;
import lombok.Data;

/**
 * @author loong
 * @date 2024/12/10 10:44
 */
@Data
public class UserPermissionRequest {

    private ResourceType resourceType;
    private String resourceId;

}
