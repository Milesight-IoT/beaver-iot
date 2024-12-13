package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineComponentManager;
import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.definition.ComponentOutputDefinition;
import com.milesight.beaveriot.rule.support.JSONHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leon
 */
public class ComponentDefinitionCache implements BeanFactoryPostProcessor {

    private static final Map<String, String> COMPONENT_DEFINITION_SCHEMA_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Collection<ComponentOutputDefinition>> COMPONENT_DEFINITION_OUTPUT_CACHE = new ConcurrentHashMap<>();
    private static ConfigurableListableBeanFactory beanFactory;

    public static String loadSchema(String componentName) {
        return COMPONENT_DEFINITION_SCHEMA_CACHE.computeIfAbsent(componentName, key ->
                beanFactory.getBean(RuleEngineComponentManager.class).getComponentDefinitionSchema(componentName));
    }

    public static ComponentDefinition load(String componentName) {
        String schema = COMPONENT_DEFINITION_SCHEMA_CACHE.computeIfAbsent(componentName, key ->
                beanFactory.getBean(RuleEngineComponentManager.class).getComponentDefinitionSchema(componentName));
        return JSONHelper.fromJSON(schema, ComponentDefinition.class);
    }

    public static Collection<ComponentOutputDefinition> loadOutputArguments(String componentName) {
        return COMPONENT_DEFINITION_OUTPUT_CACHE.computeIfAbsent(componentName, key -> load(componentName).getOutputProperties().values());
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ComponentDefinitionCache.beanFactory = beanFactory;
    }
}
