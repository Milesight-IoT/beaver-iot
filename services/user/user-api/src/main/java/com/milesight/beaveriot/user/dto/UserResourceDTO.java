package com.milesight.beaveriot.user.dto;

import com.milesight.beaveriot.user.enums.ResourceType;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/11/21 8:47
 */
@Data
public class UserResourceDTO {

    private boolean hasAllResource;
    private Map<ResourceType, List<String>> resource;

}
