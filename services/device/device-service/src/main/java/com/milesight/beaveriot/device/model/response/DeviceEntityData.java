package com.milesight.beaveriot.device.model.response;

import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.model.EntityTag;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DeviceEntityData {
    private String id;
    private String key;
    private String name;
    private EntityType type;
    private Map<String, Object> valueAttribute;
    private EntityValueType valueType;
    private String description;
    private List<EntityTag> entityTags;
}
