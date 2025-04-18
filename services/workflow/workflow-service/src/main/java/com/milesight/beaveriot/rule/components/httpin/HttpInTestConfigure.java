package com.milesight.beaveriot.rule.components.httpin;

import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.stereotype.Component;

/**
 * HttpInTestConfigure class.
 *
 * @author simon
 * @date 2025/4/17
 */
@Component
public class HttpInTestConfigure implements RuleEngineRouteConfigurer {
    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                TenantContext.setTenantId("default");
                RouteDefinition exchangeUpRoute = from("httpIn:123456?url=path/to/{param2}&method=POST")
                        .log("${body['pathParam.param2']}");
                exchangeUpRoute.setId("httpIn-test");
            }
        });
    }
}
