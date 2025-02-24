package com.milesight.beaveriot.context.security;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * @author leon
 */
@SuperBuilder
@Getter
public class TenantId implements Serializable {

    private final String tenantId;

    public TenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}