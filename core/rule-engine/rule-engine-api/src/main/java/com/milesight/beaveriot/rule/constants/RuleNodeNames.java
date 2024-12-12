package com.milesight.beaveriot.rule.constants;

/**
 * @author leon
 */
public interface RuleNodeNames {

    String innerExchangeDownFlow = "direct:innerExchangeDownFlow";
    String innerExchangeUpFlow = "direct:innerExchangeUpFlow";
    String innerExchangeValidator = "innerExchangeValidator";
    String innerSyncCallPredicate = "innerSyncCallPredicate";
    String innerEventHandlerAction = "innerEventHandlerAction";
    String innerExchangeSaveAction = "innerExchangeSaveAction";
    String innerEventSubscribeAction = "innerEventSubscribeAction";
    String innerParallelSplitter = "innerParallelSplitter";

    String CAMEL_DIRECT = "direct";
    String CAMEL_CHOICE = "choice";

}