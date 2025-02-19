package com.milesight.beaveriot.context.security;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.SuperBuilder;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

/**
 * @author leon
 */
@SuperBuilder
public class TenantContext {

    public static final String HEADER_TENANT_ID = "TENANT_ID";

    public static final String TENANT_ID = "tenantId";

    private static final TransmittableThreadLocal<TenantId> tenantThreadLocal = new TransmittableThreadLocal<>();

    public static boolean containsTenant() {
        return tenantThreadLocal.get() != null && !ObjectUtils.isEmpty(tenantThreadLocal.get().getTenantId());
    }
    public static Long getTenantId() {
        TenantId tenantId = tenantThreadLocal.get();
        if (tenantId == null || ObjectUtils.isEmpty(tenantId.getTenantId())) {
            throw new IllegalArgumentException("TenantContext is not set");
        }
        return  tenantId.getTenantId();
    }

    public static Optional<Long> tryGetTenantId() {
        TenantId tenantId = tenantThreadLocal.get();
        if (tenantId == null || ObjectUtils.isEmpty(tenantId.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(tenantId.getTenantId());
    }

    public static void setTenantId(Long tenantId) {
        tenantThreadLocal.set(new TenantId(tenantId));
    }

    public static void clear() {
        tenantThreadLocal.remove();
    }

}
