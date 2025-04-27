package com.milesight.beaveriot.context.constants;

/**
 * Cache key constant definition， for example: redis
 * Keys separated by colons
 * @author leon
 */
public class CacheKeyConstants {
    private CacheKeyConstants() {}

    public static final String PRE_SIGN_CACHE_NAME = "resource:data-pre-sign";

    public static final String RESOURCE_DATA_CACHE_NAME = "resource:data";

}
