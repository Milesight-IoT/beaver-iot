package com.milesight.beaveriot.rule.constants;

/**
 * @author leon
 */
public interface ExchangeHeaders {

    /**
     * Used to identify whether the current Exchange is a test case
     */
    String TRACE_FOR_TEST = "CamelTraceForTest";

    /**
     * Logs have been collected，do not collect again
     */
    String TRACE_HAS_COLLECTED = "CamelTraceHasCollected";

    /**
     * Save tracking result information
     */
    String TRACE_RESPONSE = "CamelTraceResponse";

    /**
     * Direct exchange to the workflow and pass the entity
     */
    String DIRECT_EXCHANGE_ENTITY = "CamelDirectExchangeEntity";

    /**
     * camel route id
     */
    String EXCHANGE_FLOW_ID = "CamelExchangeFlowId";

    String GRAPH_CHOICE_MATCH_ID = "CamelGraphChoiceMatchId";

    String EXCHANGE_LATEST_TRACE_ID = "CamelExchangeLatestNodeId";

}
