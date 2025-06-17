package com.milesight.beaveriot.context.constants;

/**
 * Cache key constant definition， for example: redis
 * Keys separated by colons
 *
 * @author leon
 */
public class CacheKeyConstants {
    private CacheKeyConstants() {
    }

    public static final String PRE_SIGN_CACHE_NAME = "resource:data-pre-sign";

    public static final String RESOURCE_DATA_CACHE_NAME = "resource:data";
    public static final String USER_MENUS_CACHE_NAME_PREFIX = "user:menus";
    public static final String ENTITY_PERMISSION_CACHE_NAME_PREFIX = "entity:permission";
    public static final String DEVICE_PERMISSION_CACHE_NAME_PREFIX = "device:permission";
    public static final String DASHBOARD_PERMISSION_CACHE_NAME_PREFIX = "dashboard:permission";
    public static final String INTEGRATION_PERMISSION_CACHE_NAME_PREFIX = "integration:permission";
    public static final String ENTITY_LATEST_VALUE_CACHE_NAME = "entity:latest-value";

    public static final String TENANT_EXPRESSION = "T(com.milesight.beaveriot.context.security.TenantContext).getTenantId()";


}
