package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineComponentManager;
import com.milesight.beaveriot.rule.configuration.RuleProperties;
import com.milesight.beaveriot.rule.flow.definition.ComponentDefinitionLoader;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.definition.BaseDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author leon
 */
@Slf4j
public class DefaultRuleEngineComponentManager implements RuleEngineComponentManager {

    private RuleProperties ruleProperties;
    private ObjectProvider<ComponentDefinitionLoader> componentDefinitionLoaderProviders;

    public DefaultRuleEngineComponentManager(RuleProperties ruleProperties, ObjectProvider<ComponentDefinitionLoader> componentDefinitionLoaderProviders) {
        this.ruleProperties = ruleProperties;
        this.componentDefinitionLoaderProviders = componentDefinitionLoaderProviders;
    }

    @Override
    public Map<String, List<BaseDefinition>> getDeclaredComponents() {
        return ruleProperties.getComponents();
    }

    @Override
    public RuleLanguage getDeclaredLanguages() {
        return ruleProperties.getLanguages();
    }

    @Override
    public String getComponentDefinitionSchema(String componentName) {
        String jsonSchema = componentDefinitionLoaderProviders.orderedStream()
                .map(loader -> {
                    try {
                        return loader.loadComponentDefinitionSchema(componentName);
                    } catch (Exception e) {
                        log.error("Failed to load component definition for {}", componentName, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        Assert.notNull(jsonSchema, "Component definition not found: " + componentName);
        return jsonSchema;
    }

}
