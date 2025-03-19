package com.milesight.beaveriot.rule.manager;

import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.manager.support.WorkflowTenantCache;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.spi.RoutePolicy;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
@Component
public class TenantRuleEngineRouteConfigurer implements RuleEngineRouteConfigurer {

    @Override
    public void customizeRoute(CamelContext context) throws Exception {

        context.addRoutePolicyFactory((camelContext, routeId, route) -> new TenantRoutePolicy()) ;
    }

    public class TenantRoutePolicy implements RoutePolicy {

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
                    TenantContext.setTenantId(tenantId);
                    if (ObjectUtils.isEmpty(exchange.getProperty(ExchangeContextKeys.SOURCE_TENANT_ID))) {
                        exchange.setProperty(ExchangeContextKeys.SOURCE_TENANT_ID, tenantId);
                    }
                }
            }
        }

        @Override
        public void onExchangeDone(Route route, Exchange exchange) {
        }
    }
}
