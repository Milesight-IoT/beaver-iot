package com.milesight.beaveriot.context.configuration;

import com.milesight.beaveriot.context.integration.GenericExchangeFlowExecutor;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author leon
 */
@Configuration
public class GenericContextAutoConfiguration {

    @Bean
    public SpringContext springContext() {
        return new SpringContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericExchangeFlowExecutor genericExchangeFlowExecutor(RuleEngineExecutor ruleEngineExecutor) {
        return new GenericExchangeFlowExecutor(ruleEngineExecutor);
    }
}
