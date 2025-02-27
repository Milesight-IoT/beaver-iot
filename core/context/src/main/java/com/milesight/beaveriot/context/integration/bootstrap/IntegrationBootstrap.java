package com.milesight.beaveriot.context.integration.bootstrap;

import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import org.apache.camel.CamelContext;

/**
 * @author leon
 */
public interface IntegrationBootstrap extends RuleEngineRouteConfigurer {

    void onPrepared(Integration integrationConfig);

    void onStarted(Integration integrationConfig);

    void onDestroy(Integration integrationConfig);

    default void onEnabled(String tenantId, Integration integrationConfig) {
    }

    default void onDisabled(String tenantId, Integration integrationConfig) {
    }

    @Override
    default void customizeRoute(CamelContext context) throws Exception {
    }

}

