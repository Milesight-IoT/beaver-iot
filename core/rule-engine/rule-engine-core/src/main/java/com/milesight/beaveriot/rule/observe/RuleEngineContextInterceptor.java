package com.milesight.beaveriot.rule.observe;

import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
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
        if (definition.getParent() instanceof RouteDefinition routeDefinition) {
            String fromId = routeDefinition.getInput().getId();
            if (StringUtils.hasText(fromId) && fromId.startsWith(RuleFlowIdGenerator.FLOW_ID_PREFIX)) {
                String fromNodeId = RuleFlowIdGenerator.removeNamespacedId(routeDefinition.getId(), fromId);
                exchange.setProperty(fromNodeId, exchange.getIn().getBody());
                exchange.setProperty(ExchangeHeaders.EXCHANGE_FLOW_ID, exchange.getFromRouteId());
            }
        }
    }

    protected void cacheOutputArguments(NamedNode definition, Exchange exchange) {
        if (definition.getId().startsWith(RuleFlowIdGenerator.FLOW_ID_PREFIX)) {
            String configNodeId = RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), definition.getId());
            exchange.setProperty(configNodeId, exchange.getIn().getBody());
        }
    }

}