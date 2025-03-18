package com.milesight.beaveriot.rule.manager.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leon
 */
public enum WorkflowTenantCache {

    INSTANCE;

    private Map<String, String> tenantHolder = new ConcurrentHashMap<>();

    public void put(String workflowId, String tenantId) {
        tenantHolder.put(workflowId, tenantId);
    }

    public String get(String workflowId) {
        return tenantHolder.get(workflowId);
    }

    public void remove(String workflowId) {
        tenantHolder.remove(workflowId);
    }

}
