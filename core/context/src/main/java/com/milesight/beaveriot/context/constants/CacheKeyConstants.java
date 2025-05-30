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

    public static final String ENTITY_LATEST_VALUE_CACHE_NAME = "entity:latest-value";

    public static final String TENANT_PREFIX = "T(com.milesight.beaveriot.context.security.TenantContext).getTenantId()";

    public static final String USER_ID_TO_MENUS = "user:menus:v2";

    public static final String USER_ID_TO_ROLES = "user:roles:v1";

    public static final String ROLE_ID_TO_USERS = "role:users:v1";

    public static final String ROLE_ID_TO_RESOURCES = "role:resources:v1";

    public static final String ENTITY_ID_TO_KEY = "entity:key:v1";

    public static final String INTEGRATION_ID_TO_DEVICE = "integration:device:v1";

}
