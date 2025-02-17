package com.milesight.beaveriot.context.security;

import com.milesight.beaveriot.base.exception.ServiceException;
import lombok.experimental.SuperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
@SuperBuilder
public class TenantContext {

    public static final String HEADER_TENANT_ID = "TENANT_ID";

    public static final String TENANT_ID = "tenantId";

    private static final ThreadLocal<TenantId> tenantThreadLocal = new ThreadLocal<>();

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

    public static void setTenantId(Long tenantId) {
        Assert.notNull(tenantId, "tenantId must not be null");
        tenantThreadLocal.set(new TenantId(tenantId));
    }

    public static void clear() {
        tenantThreadLocal.remove();
    }

}
