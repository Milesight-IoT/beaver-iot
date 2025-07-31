package com.milesight.beaveriot.rule.manager.support;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.semaphore.DistributedSemaphore;
import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/7/24 16:20
 **/
@Slf4j
@Component
public class TenantWorkflowRateLimiter {
    private static final int DEFAULT_SEMAPHORE_SIZE = 5;
    private final WorkflowRateLimitConfig workflowRateLimitConfig;
    private Map<String, Integer> tenantTypeSemaphorePermitsMap;
    private final IUserFacade userFacade;
    private final DistributedSemaphore distributedSemaphore;

    public TenantWorkflowRateLimiter(WorkflowRateLimitConfig workflowRateLimitConfig, IUserFacade userFacade, DistributedSemaphore distributedSemaphore) {
        this.workflowRateLimitConfig = workflowRateLimitConfig;
        this.userFacade = userFacade;
        this.distributedSemaphore = distributedSemaphore;
        initTenantTypeSemaphorePermitsMap();
        initTenantSemaphore();
    }

    public String acquire(String tenantId) {
        if (!workflowRateLimitConfig.isEnabled()) {
            return "";
        }
        return distributedSemaphore.acquire(getKey(tenantId), Duration.ofMillis(workflowRateLimitConfig.getTimeout()));
    }

    public void release(String tenantId, String permitId) {
        if (!workflowRateLimitConfig.isEnabled()) {
            return;
        }
        distributedSemaphore.release(getKey(tenantId), permitId);
    }

    private void initTenantTypeSemaphorePermitsMap() {
        tenantTypeSemaphorePermitsMap = Maps.newConcurrentMap();
        if (!CollectionUtils.isEmpty(workflowRateLimitConfig.getTenantConcurrency())) {
            tenantTypeSemaphorePermitsMap.putAll(workflowRateLimitConfig.getTenantConcurrency());
        }
    }

    private void initTenantSemaphore() {
        List<TenantDTO> tenants = userFacade.getAllTenants();
        if (tenants != null) {
            tenants.forEach(tenant -> {
                TenantType tenantType = getTenantTypeByTenantId(tenant.getTenantId());
                int semaphorePermits = getSemaphorePermitsByTenantType(tenantType);
                initWorkflowSemaphore(tenant.getTenantId(), semaphorePermits);
            });
        }
    }

    private void initWorkflowSemaphore(String tenantId, int semaphorePermits) {
        distributedSemaphore.initPermits(getKey(tenantId), semaphorePermits);
    }

    private String getKey(String tenantId) {
        return "workflow:semaphore:" + tenantId;
    }

    @SuppressWarnings("unused")
    private TenantType getTenantTypeByTenantId(String tenantId) {
        // Get tenant type from tenantId
        return TenantType.DEFAULT;
    }

    private int getSemaphorePermitsByTenantType(TenantType tenantType) {
        return tenantTypeSemaphorePermitsMap.getOrDefault(tenantType.name().toLowerCase(), DEFAULT_SEMAPHORE_SIZE);
    }

    public enum TenantType {
        DEFAULT
    }
}