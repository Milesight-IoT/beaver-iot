package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.user.enums.ResourceType;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/22 13:40
 */
@Data
public class RoleResourceListRequest extends GenericPageRequest {

    private ResourceType resourceType;

}
