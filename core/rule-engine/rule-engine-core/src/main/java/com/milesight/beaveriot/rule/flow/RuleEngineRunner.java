package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.AutowiredTypeConverter;
import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.configuration.RuleProperties;
import com.milesight.beaveriot.rule.exception.RuleEngineException;
import com.milesight.beaveriot.rule.flow.graph.GraphChoiceDefinition;
import com.milesight.beaveriot.rule.flow.graph.GraphChoiceReifier;
import com.milesight.beaveriot.rule.flow.graph.GraphProcessorDefinition;
import com.milesight.beaveriot.rule.flow.graph.GraphProcessorReifier;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.spi.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * @author leon
 */
@SuppressWarnings("java:S3740")
public class RuleEngineRunner implements SmartInitializingSingleton {

    private ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers;
    private CamelContext camelContext;
    private CamelRuleEngineExecutor camelRuleEngineExecutor;
    private RuleProperties ruleProperties;
    private ObjectProvider<Tracer> tracerObjectProvider;
    private ObjectProvider<AutowiredTypeConverter> autowiredTypeConverters;

    public RuleEngineRunner(ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers, CamelRuleEngineExecutor camelRuleEngineExecutor, CamelContext context, RuleProperties ruleProperties, ObjectProvider<Tracer> tracerObjectProvider, ObjectProvider<AutowiredTypeConverter> autowiredTypeConverters) {
        this.ruleEngineRouteConfigurers = ruleEngineRouteConfigurers;
        this.camelRuleEngineExecutor = camelRuleEngineExecutor;
        this.camelContext = context;
        this.ruleProperties = ruleProperties;
        this.tracerObjectProvider = tracerObjectProvider;
        this.autowiredTypeConverters = autowiredTypeConverters;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    @SneakyThrows
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

        if (ruleProperties.isEnabledTracing() && tracerObjectProvider.getIfAvailable() != null) {
            camelContext.setTracing(true);
            camelContext.setTracer(tracerObjectProvider.getIfAvailable());
        }

        //register graph definition
        registerGraphDefinition();

        //set camel context to camelRuleEngineExecutor
        camelRuleEngineExecutor.initializeCamelContext(camelContext);

        //register autowired type converters
        autowiredTypeConverters.stream().forEach(autowiredTypeConverter -> camelContext.getTypeConverterRegistry().addConverter(autowiredTypeConverter.getTypeConvertible(), autowiredTypeConverter));
    }

    protected void registerGraphDefinition() {
        ProcessorReifier.registerReifier(GraphProcessorDefinition.class, GraphProcessorReifier::new);
        ProcessorReifier.registerReifier(GraphChoiceDefinition.class, GraphChoiceReifier::new);
    }
}
