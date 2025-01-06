package com.milesight.beaveriot.rule.configuration;

import com.milesight.beaveriot.rule.AutowiredTypeConverter;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.flow.*;
import com.milesight.beaveriot.rule.flow.definition.AnnotationComponentDefinitionLoader;
import com.milesight.beaveriot.rule.flow.definition.CamelComponentDefinitionLoader;
import com.milesight.beaveriot.rule.flow.definition.ComponentDefinitionLoader;
import com.milesight.beaveriot.rule.flow.definition.CustomizeJsonComponentDefinitionLoader;
import com.milesight.beaveriot.rule.flow.parallel.ParallelSplitter;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author leon
 */
@SuppressWarnings("java:S3740")
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
    public RuleEngineRunner ruleEngineRunner(ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers, CamelRuleEngineExecutor ruleEngineExecutor, CamelContext context, RuleProperties ruleProperties, ObjectProvider<Tracer> tracerObjectProvider, ObjectProvider<AutowiredTypeConverter> autowiredTypeConverters) {
        return new RuleEngineRunner(ruleEngineRouteConfigurers, ruleEngineExecutor, context, ruleProperties, tracerObjectProvider, autowiredTypeConverters);
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultRuleEngineLifecycleManager ruleEngineLifecycleManager(CamelContext context, RuleEngineExecutor ruleEngineExecutor) {
        return new DefaultRuleEngineLifecycleManager(context, ruleEngineExecutor);
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

    @Bean
    @ConditionalOnMissingBean
    public ComponentDefinitionCache componentDefinitionCache() {
        return new ComponentDefinitionCache();
    }

    @Bean(RuleNodeNames.innerParallelSplitter)
    @ConditionalOnMissingBean
    public ParallelSplitter parallelSplitter() {
        return new ParallelSplitter();
    }

}
