package com.milesight.beaveriot.rule.manager.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/7/25 9:38
 **/
@Data
@Component
@ConfigurationProperties(prefix = "workflow.rate-limit")
public class WorkflowRateLimitConfig {
    private boolean enabled = false;
    private long timeout = 5000;
    private Map<String, Integer> tenantConcurrency;
}