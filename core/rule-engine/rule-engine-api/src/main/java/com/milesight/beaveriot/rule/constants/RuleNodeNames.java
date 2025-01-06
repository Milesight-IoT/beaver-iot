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

    String CAMEL_DIRECT = "direct";
    String CAMEL_CHOICE = "choice";

}
