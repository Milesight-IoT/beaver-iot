package com.milesight.beaveriot.rule.constants;

/**
 * @author leon
 */
public interface RuleNodeNames {

    String innerExchangeRouteId = "innerExchangeRoute";
    String innerExchangeFlow = "direct:innerExchangeFlow";
    String innerExchangeValidator = "innerExchangeValidator";
    String innerSyncCallPredicate = "innerSyncCallPredicate";
    String innerEventHandlerAction = "innerEventHandlerAction";
    String innerExchangeSaveAction = "innerExchangeSaveAction";
    String innerEventSubscribeAction = "innerEventSubscribeAction";
    String innerParallelSplitter = "innerParallelSplitter";
    String innerDirectExchangePredicate = "innerDirectExchangePredicate";
    String innerWorkflowTriggerByEntity = "innerWorkflowTriggerByEntity";

    String CAMEL_TRIGGER = "trigger";
    String CAMEL_DIRECT = "direct";
    String CAMEL_CHOICE = "choice";
    String CAMEL_OUTPUT = "output";
    String CAMEL_BEAN = "bean";
    String CAMEL_BEAN_SCHEMA = "bean:";
    String CAMEL_BEAN_PREFIX = "bean.";

}
