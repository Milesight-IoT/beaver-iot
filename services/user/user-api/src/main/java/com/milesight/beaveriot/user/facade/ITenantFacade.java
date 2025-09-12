package com.milesight.beaveriot.user.facade;

/**
 * author: Luxb
 * create: 2025/8/20 13:52
 **/
public interface ITenantFacade {
    void runWithAllTenants(Runnable runnable);
}
