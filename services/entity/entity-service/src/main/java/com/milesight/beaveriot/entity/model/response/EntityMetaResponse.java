package com.milesight.beaveriot.entity.model.response;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.Data;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/22 13:27
 */
@Data
public class EntityMetaResponse {

    private String key;
    private String name;
    private EntityType type;
    private AccessMod accessMod;
    private Map<String, Object> valueAttribute;
    private EntityValueType valueType;

}
