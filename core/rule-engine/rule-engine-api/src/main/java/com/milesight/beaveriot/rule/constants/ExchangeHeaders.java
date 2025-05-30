package com.milesight.beaveriot.rule.constants;

import org.apache.camel.Exchange;

import java.util.HashMap;
import java.util.Map;

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

    String EXCHANGE_DONE_ADVICE_FLAG = "CamelExchangeDoneAdviceFlag";

    /**
     * property key for Exchange execution times in the route, Passing in global routing
     */
    String EXCHANGE_EXECUTION_REPEAT_COUNT = "CamelExchangeExecutionRepeatCount";

    String EXCHANGE_ROOT_FLOW_ID = "CamelExchangeRootFlowId";

    /**
     * Supports custom log input variable definitions. When obtaining logs,
     * the variable value defined here is preferred, and stored in Map, where the Key is the node ID.
     */
    String EXCHANGE_CUSTOM_INPUT_LOG_VARIABLES = "CamelCustomInputLogVariables";

    /**
     * Supports custom output variable definitions. When obtaining logs, the variable value defined here is preferred,
     * and stored in Map, where the Key is the node ID.
     */
    String EXCHANGE_CUSTOM_OUTPUT_LOG_VARIABLES = "CamelCustomOutputLogVariables";

    String EXCHANGE_OUTPUT_PROCESSOR = "CamelOutputProcessor";

    public static boolean containsMapProperty(Exchange exchange, String propertyName, String mapKey) {
        Object property = exchange.getProperty(propertyName);
        return property != null && property instanceof Map<?,?> propertyMap && propertyMap.containsKey(mapKey);
    }

    public static Object getMapProperty(Exchange exchange, String propertyName, String mapKey) {
        Object property = exchange.getProperty(propertyName);
        if (property != null && property instanceof Map<?,?> propertyMap) {
            return propertyMap.get(mapKey);
        }
        return null;
    }

    public static void putMapProperty(Exchange exchange, String propertyName, String mapKey, Object mapValue) {
        Object property = exchange.getProperty(propertyName);
        if (property != null && property instanceof Map<?,?> propertyMap) {
            ((Map<String,Object>)propertyMap).put(mapKey, mapValue);
        } else {
            Map<String, Object> newMap = new HashMap<>();
            newMap.put(mapKey, mapValue);
            exchange.setProperty(propertyName, newMap);
        }
    }

}
