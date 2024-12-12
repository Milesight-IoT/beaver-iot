package com.milesight.beaveriot.rule.observe;

import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.spi.InterceptStrategy;

/**
 * @author leon
 */
public class RuleEngineOutputInterceptor implements InterceptStrategy {

    @Override
    public Processor wrapProcessorInInterceptors(CamelContext context, NamedNode definition, Processor target, Processor nextTarget) throws Exception {
        return exchange -> {

            target.process(exchange);

            cacheOutputArguments(definition, exchange);
        };
    }

    protected void cacheOutputArguments(NamedNode definition, Exchange exchange) {
        if(definition.getId().startsWith(RuleFlowIdGenerator.FLOW_ID_PREFIX)) {
            String configNodeId = RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), definition.getId());
            exchange.setProperty(configNodeId, exchange.getIn().getBody());
        }
    }

}
