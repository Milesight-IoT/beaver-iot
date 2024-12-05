package com.milesight.beaveriot.rule.configuration;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.flow.CamelRuleEngineExecutor;
import com.milesight.beaveriot.rule.flow.DefaultRuleEngineComponentManager;
import com.milesight.beaveriot.rule.flow.DefaultRuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.flow.RuleEngineRunner;
import com.milesight.beaveriot.rule.flow.definition.AnnotationComponentDefinitionLoader;
import com.milesight.beaveriot.rule.flow.definition.CamelComponentDefinitionLoader;
import com.milesight.beaveriot.rule.flow.definition.ComponentDefinitionLoader;
import com.milesight.beaveriot.rule.flow.definition.CustomizeJsonComponentDefinitionLoader;
import com.milesight.beaveriot.rule.trace.RuleEngineTracer;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author leon
 */
@EnableConfigurationProperties(RuleProperties.class)
@Configuration
public class RuleEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CamelRuleEngineExecutor ruleEngineExecutor() {
        return new CamelRuleEngineExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleEngineRunner ruleEngineRunner(ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers, CamelRuleEngineExecutor ruleEngineExecutor, CamelContext context) {
        return new RuleEngineRunner(ruleEngineRouteConfigurers, ruleEngineExecutor, context);
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultRuleEngineLifecycleManager ruleEngineLifecycleManager(DefaultCamelContext camelContext, ProducerTemplate producerTemplate) {
        return new DefaultRuleEngineLifecycleManager(camelContext, producerTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultRuleEngineComponentManager ruleEngineComponentManager(RuleProperties ruleProperties, ObjectProvider<ComponentDefinitionLoader> componentDefinitionLoaderProviders) {
        return new DefaultRuleEngineComponentManager(ruleProperties, componentDefinitionLoaderProviders);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnnotationComponentDefinitionLoader annotationComponentDefinitionLoader() {
        return new AnnotationComponentDefinitionLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    public CamelComponentDefinitionLoader camelComponentDefinitionLoader(CamelContext context) {
        return new CamelComponentDefinitionLoader(context);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomizeJsonComponentDefinitionLoader customizeJsonComponentDefinitionLoader(RuleProperties ruleProperties) {
        return new CustomizeJsonComponentDefinitionLoader(ruleProperties);
    }

    @ConditionalOnProperty(value = "camel.rule.enabled-tracing", matchIfMissing = true)
    public class RuleEngineTraceConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public RuleEngineTracer ruleEngineTracer(ApplicationEventPublisher applicationEventPublisher, RuleProperties ruleProperties) {
            return new RuleEngineTracer(applicationEventPublisher, ruleProperties);
        }
    }
}
