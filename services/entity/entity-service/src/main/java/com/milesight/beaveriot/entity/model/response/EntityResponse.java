package com.milesight.beaveriot.entity.model.response;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.Data;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/21 11:00
 */
@Data
public class EntityResponse {

    private String deviceName;

    private String integrationName;

    private String entityId;

    private AccessMod entityAccessMod;

    private String entityKey;

    private EntityType entityType;

    private String entityName;

    private Map<String, Object> entityValueAttribute;

    private EntityValueType entityValueType;

    private Boolean entityIsCustomized;

    private Long entityCreatedAt;

    private Long entityUpdatedAt;

}
