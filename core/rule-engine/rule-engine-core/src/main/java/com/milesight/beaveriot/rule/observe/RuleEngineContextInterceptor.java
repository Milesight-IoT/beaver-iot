package com.milesight.beaveriot.rule.observe;

import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.flow.graph.GraphProcessorDefinition;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.SneakyThrows;
import org.apache.camel.*;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.support.AsyncProcessorSupport;
import org.springframework.util.StringUtils;

/**
 * @author leon
 */
public class RuleEngineContextInterceptor implements InterceptStrategy {

    @Override
    public Processor wrapProcessorInInterceptors(CamelContext context, NamedNode definition, Processor target, Processor nextTarget) throws Exception {
        return new AsyncProcessorSupport() {

            @SneakyThrows
            @Override
            public boolean process(Exchange exchange, AsyncCallback callback) {

                cacheFromArguments(definition, exchange);

                target.process(exchange);

                cacheOutputArguments(definition, exchange);

                callback.done(true);

                return true;
            }
        };
    }

    private void cacheFromArguments(NamedNode definition, Exchange exchange) {
        // if the definition is a route definition, then cache the input arguments
        RouteDefinition routeDefinition = null;
        if (definition.getParent() instanceof GraphProcessorDefinition graphProcessorDefinition) {
            routeDefinition = (RouteDefinition) graphProcessorDefinition.getParent();
        } else if (definition.getParent() instanceof RouteDefinition) {
            routeDefinition = (RouteDefinition) definition.getParent();
        } else {
            return;
        }

        String fromId = routeDefinition.getInput().getId();
        if (StringUtils.hasText(fromId) && fromId.startsWith(RuleFlowIdGenerator.FLOW_ID_PREFIX)) {
            String fromNodeId = RuleFlowIdGenerator.removeNamespacedId(routeDefinition.getId(), fromId);
            if (exchange.getProperties().containsKey(fromNodeId)) {
                return;
            }
            exchange.setProperty(fromNodeId, exchange.getIn().getBody());
            exchange.setProperty(ExchangeHeaders.EXCHANGE_FLOW_ID, exchange.getFromRouteId());
        }
    }

    protected void cacheOutputArguments(NamedNode definition, Exchange exchange) {
        if (definition.getId().startsWith(RuleFlowIdGenerator.FLOW_ID_PREFIX)) {
            String configNodeId = RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), definition.getId());
            exchange.setProperty(configNodeId, exchange.getIn().getBody());
        }
    }

}
