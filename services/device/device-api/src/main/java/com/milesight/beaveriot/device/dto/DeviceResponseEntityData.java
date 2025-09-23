package com.milesight.beaveriot.device.dto;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceResponseEntityData {
    private String id;
    private String key;
    private String name;
    private EntityType type;
    private Map<String, Object> valueAttribute;
    private EntityValueType valueType;
    private AccessMod accessMod;
    private String description;
    private String parent;
}
