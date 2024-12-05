package com.milesight.beaveriot.rule;

import org.apache.camel.CamelContext;

/**
 * @author leon
 */
public interface RuleEngineRouteConfigurer {

    void customizeRoute(CamelContext context) throws Exception;

}

