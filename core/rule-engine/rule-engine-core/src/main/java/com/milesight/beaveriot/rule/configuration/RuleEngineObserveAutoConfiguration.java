package com.milesight.beaveriot.rule.configuration;

import com.milesight.beaveriot.rule.observe.RuleEngineContextInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author leon
 */
@ConditionalOnProperty(value = "camel.rule.enabled-observe", matchIfMissing = true)
@EnableConfigurationProperties(RuleProperties.class)
@Configuration
public class RuleEngineObserveAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RuleEngineContextInterceptor ruleEngineContextInterceptor() {
        return new RuleEngineContextInterceptor();
    }

}
