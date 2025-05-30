package com.milesight.beaveriot.permission.context;

import com.milesight.beaveriot.permission.enums.ColumnDataType;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author loong
 * @date 2024/12/5 15:07
 */
public class DataAspectContext {

    private static final ThreadLocal<Map<String, TenantContext>> tenantContextHolder = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, DataPermissionContext>> DataPermissionHolder = new ThreadLocal<>();

    public static void setTenantContext(String tableName, TenantContext tenantContext) {
        Map<String, TenantContext> tenantContextMap = tenantContextHolder.get();
        if (tenantContextMap == null) {
            tenantContextMap = new ConcurrentHashMap<>();
        }
        tenantContextMap.put(tableName, tenantContext);
        tenantContextHolder.set(tenantContextMap);
    }

    public static void setDataPermissionContext(String tableName, DataPermissionContext dataPermissionContext) {
        Map<String, DataPermissionContext> dataPermissionContextMap = DataPermissionHolder.get();
        if (dataPermissionContextMap == null) {
            dataPermissionContextMap = new ConcurrentHashMap<>();
        }
        dataPermissionContextMap.put(tableName, dataPermissionContext);
        DataPermissionHolder.set(dataPermissionContextMap);
    }

    public static TenantContext getTenantContext(String tableName) {
        return tenantContextHolder.get() != null ? tenantContextHolder.get().get(tableName) : null;
    }

    public static DataPermissionContext getDataPermissionContext(String tableName) {
        return DataPermissionHolder.get() != null ? DataPermissionHolder.get().get(tableName) : null;
    }

    public static boolean isTenantEnabled(String tableName) {
        return tenantContextHolder.get() != null && tenantContextHolder.get().get(tableName) != null && tenantContextHolder.get().get(tableName).getTenantId() != null;
    }

    public static boolean isDataPermissionEnabled(String tableName) {
        return DataPermissionHolder.get() != null && DataPermissionHolder.get().get(tableName) != null && DataPermissionHolder.get().get(tableName).getDataIds() != null && !DataPermissionHolder.get().get(tableName).getDataIds().isEmpty();
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
        private String tenantId;
        private String tenantColumnName;
    }

    @Builder
    @Getter
    public static class DataPermissionContext {
        private List<String> dataIds;
        private ColumnDataType dataType;
        private String dataColumnName;
    }

}
