package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */
@Component
public class ExchangeRuleEngineRouteConfigurer implements RuleEngineRouteConfigurer {
    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                RouteDefinition exchangeUpRoute = from(RuleNodeNames.innerExchangeFlow);
                exchangeUpRoute.setId(RuleNodeNames.innerExchangeRouteId);
                exchangeUpRoute.choice()
                        .when().method(RuleNodeNames.innerExchangeValidator)
                        .bean(RuleNodeNames.innerExchangeSaveAction)
                        .choice()
                        .when().method(RuleNodeNames.innerDirectExchangePredicate)
                        .bean(RuleNodeNames.innerWorkflowTriggerByEntity)
                        .when().method(RuleNodeNames.innerSyncCallPredicate)
                        .bean(RuleNodeNames.innerEventHandlerAction)
                        .otherwise()
                        .bean(RuleNodeNames.innerEventSubscribeAction)
                        .end()
                        .endChoice()
                        .otherwise()
                        .log("ExchangeValidator failed on innerExchangeUpFlow.")
                        .end();

            }
        });
    }
}
