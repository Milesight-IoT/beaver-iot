package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.user.enums.ResourceType;
import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/20 14:02
 */
@Data
public class RoleResourceRequest {

    private List<Resource> resources;

    @Data
    public static class Resource {
        private String id;
        private ResourceType type;
    }

}
