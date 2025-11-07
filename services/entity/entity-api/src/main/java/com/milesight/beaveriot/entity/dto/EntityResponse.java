package com.milesight.beaveriot.entity.dto;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.enums.ValueStoreMod;
import com.milesight.beaveriot.context.model.EntityTag;
import lombok.*;

import java.util.List;
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

    private ValueStoreMod entityValueStoreMod;

    private String entityKey;

    private EntityType entityType;

    private String entityName;

    private String entityParentName;

    private Map<String, Object> entityValueAttribute;

    private EntityValueType entityValueType;

    private Boolean entityIsCustomized;

    private Long entityCreatedAt;

    private Long entityUpdatedAt;

    private String entityDescription;

    private String entityLatestValue;

    private EntityDeviceGroup deviceGroup;

    private List<EntityTag> entityTags;

    private EntityWorkflowData workflowData;

}
