package com.milesight.beaveriot.context.security;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.milesight.beaveriot.context.support.KeyValidator;
import lombok.experimental.*;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

/**
 * @author leon
 */
@SuperBuilder
public class TenantContext {

    public static final String HEADER_TENANT_ID = "TENANT-ID";

    public static final String TENANT_ID = "tenantId";

    private static final TransmittableThreadLocal<TenantId> tenantThreadLocal = new TransmittableThreadLocal<>();

    public static boolean containsTenant() {
        return tenantThreadLocal.get() != null && !ObjectUtils.isEmpty(tenantThreadLocal.get().getTenantId());
    }
    public static String getTenantId() {
        TenantId tenantId = tenantThreadLocal.get();
        if (tenantId == null || ObjectUtils.isEmpty(tenantId.getTenantId())) {
            throw new IllegalArgumentException("TenantContext is not set");
        }
        return  tenantId.getTenantId();
    }

    public static Optional<String> tryGetTenantId() {
        TenantId tenantId = tenantThreadLocal.get();
        if (tenantId == null || ObjectUtils.isEmpty(tenantId.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(tenantId.getTenantId());
    }

    public static Optional<Object> tryGetTenantParam(String paramName) {
        TenantId tenantId = tenantThreadLocal.get();
        if (tenantId == null || ObjectUtils.isEmpty(tenantId.getTenantId())) {
            return Optional.empty();
        }

        return Optional.ofNullable(tenantId.getTenantParams().get(paramName));
    }

    /*
     * Be careful! Lifecycle of every param should be completed.
     */
    public static boolean tryPutTenantParam(String paramName, Object paramValue) {
        TenantId tenantId = tenantThreadLocal.get();
        if (tenantId == null || ObjectUtils.isEmpty(tenantId.getTenantId())) {
            return false;
        }

        if (paramValue == null) {
            tenantId.getTenantParams().remove(paramName);
        } else {
            tenantId.getTenantParams().put(paramName, paramValue);
        }

        return true;
    }

    public static void setTenantId(String tenantId) {
        if (!KeyValidator.isValid(tenantId)) {
            throw new IllegalArgumentException("Tenant ID '" + tenantId + "' is not valid");
        }

        TenantId oldTenant = tenantThreadLocal.get();
        if (oldTenant == null || !tenantId.equals(oldTenant.getTenantId())) {
            tenantThreadLocal.set(new TenantId(tenantId));
        } else {
            tenantThreadLocal.set(new TenantId(oldTenant));
        }
    }

    public static void clear() {
        tenantThreadLocal.remove();
    }

}
