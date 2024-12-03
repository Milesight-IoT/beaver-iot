package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.exception.RuleEngineException;
import org.apache.camel.CamelContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author leon
 */
public class RuleEngineRunner implements SmartInitializingSingleton, ApplicationContextAware {

    private ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers;
    private CamelContext camelContext;
    private ApplicationContext applicationContext;
    private CamelRuleEngineExecutor camelRuleEngineExecutor;

    public RuleEngineRunner(ObjectProvider<RuleEngineRouteConfigurer> ruleEngineRouteConfigurers,CamelRuleEngineExecutor camelRuleEngineExecutor, CamelContext context) {
        this.ruleEngineRouteConfigurers = ruleEngineRouteConfigurers;
        this.camelRuleEngineExecutor = camelRuleEngineExecutor;
        this.camelContext = context;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {

        //register all RuleNode beans to camel registry
        String[] beanNamesForAnnotation = applicationContext.getBeanNamesForAnnotation(RuleNode.class);
        for (String beanName : beanNamesForAnnotation) {
            Object bean = applicationContext.getBean(beanName);
            RuleNode ruleNode = AnnotationUtils.getAnnotation(bean.getClass(), RuleNode.class);
            if(ruleNode != null){
                camelContext.getRegistry().bind(ruleNode.name(), bean);
            }
        }

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
