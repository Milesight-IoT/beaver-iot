package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.exception.RuleEngineException;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * @author leon
 */
public class RuleEngineRunner implements SmartInitializingSingleton {

    private ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers;
    private CamelContext camelContext;
    private CamelRuleEngineExecutor camelRuleEngineExecutor;

    public RuleEngineRunner(ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers, CamelRuleEngineExecutor camelRuleEngineExecutor, CamelContext context) {
        this.ruleEngineRouteConfigurers = ruleEngineRouteConfigurers;
        this.camelRuleEngineExecutor = camelRuleEngineExecutor;
        this.camelContext = context;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void afterSingletonsInstantiated() {

        //customize route
        ruleEngineRouteConfigurers.stream().forEach(ruleEngineRouteConfigurer -> {
            try {
                ruleEngineRouteConfigurer.customizeRoute(camelContext);
            } catch (Exception e) {
                throw new RuleEngineException("Failed to configure rule engine route", e);
            }
        });

        //set camel context to camelRuleEngineExecutor
        camelRuleEngineExecutor.initializeCamelContext(camelContext);

    }
}
