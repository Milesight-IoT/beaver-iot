package com.milesight.beaveriot.rule.manager.support;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.spi.RoutePolicy;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/7/25 11:04
 **/
@Slf4j
@Component
public class TenantRoutePolicy implements RoutePolicy {
    private final TenantWorkflowRateLimiter tenantWorkflowRateLimiter;

    public TenantRoutePolicy(TenantWorkflowRateLimiter tenantWorkflowRateLimiter) {
        this.tenantWorkflowRateLimiter = tenantWorkflowRateLimiter;
    }

    @Override
    public void onInit(Route route) {
    }

    @Override
    public void onRemove(Route route) {
    }

    @Override
    public void onStart(Route route) {
    }

    @Override
    public void onStop(Route route) {
    }

    @Override
    public void onSuspend(Route route) {
    }

    @Override
    public void onResume(Route route) {
    }

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        String workflowId = route.getId();
        if (NumberUtils.isCreatable(workflowId)) {
            String tenantId = WorkflowTenantCache.INSTANCE.get(workflowId);
            if (!ObjectUtils.isEmpty(tenantId)) {
                if (tenantWorkflowRateLimiter.acquire(tenantId)) {
                    try {
                        TenantContext.setTenantId(tenantId);
                        if (ObjectUtils.isEmpty(exchange.getProperty(ExchangeContextKeys.SOURCE_TENANT_ID))) {
                            exchange.setProperty(ExchangeContextKeys.SOURCE_TENANT_ID, tenantId);
                        }
                    } finally {
                        tenantWorkflowRateLimiter.release(tenantId);
                    }
                } else {
                    String errorMessage = MessageFormat.format("Failed occurred during acquiring workflow semaphore for tenant {0}", tenantId);
                    log.error(errorMessage);
                    throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), errorMessage).build();
                }
            }
        }
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {

    }
}