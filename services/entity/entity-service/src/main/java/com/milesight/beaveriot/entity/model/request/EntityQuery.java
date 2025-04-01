package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.base.page.GenericQueryPageRequest;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/16 14:59
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EntityQuery extends GenericQueryPageRequest {

    private List<EntityType> entityType;

    private Boolean excludeChildren;

    private List<EntityValueType> entityValueType;

    private List<AccessMod> entityAccessMod;

    private List<Long> entityIds;

    private List<String> entityKeys;

    private List<String> entityNames;

    /**
     * Filter customized entities
     */
    private Boolean customized;

    /**
     * Show hidden entities
     */
    private Boolean showHidden;

}
