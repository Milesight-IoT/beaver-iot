package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
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
                from(RuleNodeNames.innerExchangeUpFlow)
                        .choice()
                        .when().method(RuleNodeNames.innerExchangeValidator)
                        .bean(RuleNodeNames.innerExchangeSaveAction)
                        .choice()
                        .when().method(RuleNodeNames.innerSyncCallPredicate)
                        .bean(RuleNodeNames.innerEventHandlerAction)
                        .otherwise()
                        .bean(RuleNodeNames.innerEventSubscribeAction)
                        .end()
                        .endChoice()
                        .otherwise()
                        .log("ExchangeValidator failed on innerExchangeUpFlow.")
                        .end();

                from(RuleNodeNames.innerExchangeDownFlow)
                        .choice()
                        .when().method(RuleNodeNames.innerExchangeValidator)
                        .bean(RuleNodeNames.innerExchangeSaveAction)
                        .choice()
//                            .when().method(RuleNodeNames.innerCustomExchangeDownHandlerPredicate)
//                                .to("direct:innerExchangeDownHandler!msc-integration")
                        .when().method(RuleNodeNames.innerSyncCallPredicate)
                        .bean(RuleNodeNames.innerEventHandlerAction)
                        .otherwise()
                        .bean(RuleNodeNames.innerEventSubscribeAction)
                        .end()
                        .endChoice()
                        .otherwise()
                        .log("ExchangeValidator failed on innerExchangeDownFlow")
                        .end();
            }
        });
    }
}
