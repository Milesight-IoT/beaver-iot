package com.milesight.beaveriot.rule.model.flow.config;

import static com.milesight.beaveriot.rule.constants.RuleNodeNames.CAMEL_CHOICE;

/**
 * @author leon
 */
public interface RuleConfig {

    String COMPONENT_CHOICE = CAMEL_CHOICE;
    String COMPONENT_CHOICE_WHEN = "choice_when";
    String COMPONENT_CHOICE_OTHERWISE = "choice_otherwise";

    String getId();

    default String getName() {
        return null;
    }

    String getComponentName();

}
