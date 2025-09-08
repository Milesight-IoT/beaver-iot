package com.milesight.beaveriot.context.api;

/**
 * author: Luxb
 * create: 2025/8/20 13:52
 **/
public interface TenantServiceProvider {
    void runWithAllTenants(Runnable runnable);
}
