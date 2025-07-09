package com.milesight.beaveriot.context.security;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author leon
 */
@SuperBuilder
@Getter
public class TenantId implements Serializable {

    private final String tenantId;

    private final Map<String, Object> tenantParams = new HashMap<>();

    public TenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}