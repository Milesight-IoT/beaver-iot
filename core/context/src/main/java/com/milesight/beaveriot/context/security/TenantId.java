package com.milesight.beaveriot.context.security;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * @author leon
 */
@SuperBuilder
@Getter
public class TenantId {

    private final Long tenantId;

    public TenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

}