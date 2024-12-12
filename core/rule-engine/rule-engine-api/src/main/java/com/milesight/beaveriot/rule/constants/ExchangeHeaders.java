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
     * Save tracking result information
     */
    String TRACE_RESPONSE = "CamelTraceResponse";

}
