package com.milesight.beaveriot.rule.manager;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.manager.support.TenantRoutePolicy;
import org.apache.camel.CamelContext;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */
@Component
public class TenantRuleEngineRouteConfigurer implements RuleEngineRouteConfigurer {
    private final TenantRoutePolicy tenantRoutePolicy;

    public TenantRuleEngineRouteConfigurer(TenantRoutePolicy tenantRoutePolicy) {
        this.tenantRoutePolicy = tenantRoutePolicy;
    }

    @Override
    public void customizeRoute(CamelContext context) {
        context.addRoutePolicyFactory((camelContext, routeId, route) -> tenantRoutePolicy) ;
    }
}
