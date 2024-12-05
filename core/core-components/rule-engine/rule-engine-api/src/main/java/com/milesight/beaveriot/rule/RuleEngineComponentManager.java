package com.milesight.beaveriot.rule;

import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.definition.BaseDefinition;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
public interface RuleEngineComponentManager {

    Map<RuleNodeType, List<BaseDefinition>> getDeclaredComponents();

    RuleLanguage getDeclaredLanguages();

    String getComponentDefinitionSchema(String name);

}
