package com.milesight.beaveriot.rule.configuration;

import com.milesight.beaveriot.rule.trace.RuleEngineTracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author leon
 */
@ConditionalOnProperty(value = "camel.rule.enabled-tracing", matchIfMissing = true)
@EnableConfigurationProperties(RuleProperties.class)
@Configuration
public class RuleEngineTraceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RuleEngineTracer ruleEngineTracer(ApplicationEventPublisher applicationEventPublisher, RuleProperties ruleProperties) {
        return new RuleEngineTracer(applicationEventPublisher, ruleProperties);
    }

}
