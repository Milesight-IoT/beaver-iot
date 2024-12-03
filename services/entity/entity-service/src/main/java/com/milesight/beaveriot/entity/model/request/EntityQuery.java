package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/16 14:59
 */
@Data
public class EntityQuery extends GenericPageRequest {

    private String keyword;
    private List<EntityType> entityType;
    private Boolean excludeChildren;
    private List<EntityValueType> entityValueType;
    private List<AccessMod> entityAccessMod;

}
