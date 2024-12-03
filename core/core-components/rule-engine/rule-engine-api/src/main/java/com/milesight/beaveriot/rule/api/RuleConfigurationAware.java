package com.milesight.beaveriot.rule.api;

/**
 * @author leon
 */
public interface RuleConfigurationAware {

    default RuleConfiguration getRuleConfiguration(){
        return null;
    }
}
