package com.milesight.beaveriot.permission.context;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author loong
 * @date 2024/12/5 15:07
 */
public class DataAspectContext {

    private static final ThreadLocal<TenantContext> tenantContextHolder = new ThreadLocal<>();
    private static final ThreadLocal<DataPermissionContext> DataPermissionHolder = new ThreadLocal<>();

    public static void setTenantContext(TenantContext tenantContext) {
        tenantContextHolder.set(tenantContext);
    }

    public static void setDataPermissionContext(DataPermissionContext dataPermissionContext) {
        DataPermissionHolder.set(dataPermissionContext);
    }

    public static TenantContext getTenantContext() {
        return tenantContextHolder.get();
    }

    public static DataPermissionContext getDataPermissionContext() {
        return DataPermissionHolder.get();
    }

    public static boolean isTenantEnabled() {
        return tenantContextHolder.get() != null && tenantContextHolder.get().getTenantId() != null;
    }

    public static boolean isDataPermissionEnabled() {
        return DataPermissionHolder.get() != null && DataPermissionHolder.get().getDataIds() != null && !DataPermissionHolder.get().getDataIds().isEmpty();
    }

    public static void clearTenantContext() {
        tenantContextHolder.remove();
    }

    public static void clearDataPermissionContext() {
        DataPermissionHolder.remove();
    }

    @Builder
    @Getter
    public static class TenantContext {
        private Long tenantId;
        private String tenantColumnName;
    }

    @Builder
    @Getter
    public static class DataPermissionContext {
        private List<Long> dataIds;
        private String dataColumnName;
    }

}
