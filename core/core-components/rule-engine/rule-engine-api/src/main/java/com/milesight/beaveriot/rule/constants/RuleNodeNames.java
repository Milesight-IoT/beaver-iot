package com.milesight.beaveriot.rule.constants;

/**
 * @author leon
 */
public interface RuleNodeNames {

    String innerExchangeDownFlow = "direct:innerExchangeDownFlow";
    String innerExchangeUpFlow = "direct:innerExchangeUpFlow";
    String innerExchangeDownHandler = "direct:innerExchangeDownHandler";
    String innerExchangeValidator = "innerExchangeValidator";
    String innerSyncCallPredicate = "innerSyncCallPredicate";
    String innerCustomExchangeDownHandlerPredicate = "innerCustomExchangeDownHandlerPredicate";
    String innerEventHandlerAction = "innerEventHandlerAction";
    String innerExchangeSaveAction = "innerExchangeSaveAction";
    String innerEventSubscribeAction = "innerEventSubscribeAction";


    @Deprecated
    String innerLatestExchangeSaveAction = "innerLatestExchangeSaveAction";
    @Deprecated
    String innerHistoryExchangeSaveAction = "innerHistoryExchangeSaveAction";
    @Deprecated
    String innerExchangeSplitter = "innerExchangeSplitter";
}
