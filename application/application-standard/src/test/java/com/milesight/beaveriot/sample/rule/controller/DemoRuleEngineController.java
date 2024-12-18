package com.milesight.beaveriot.sample.rule.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.flow.builder.RuleFlowYamlBuilder;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.flow.route.RouteNode;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author leon
 */
@Slf4j
@RestController
public class DemoRuleEngineController {

    @Autowired
    private RuleEngineLifecycleManager ruleEngineLifecycleManager;
    @Autowired
    ExchangeFlowExecutor exchangeFlowExecutor;

    @PostMapping("/public/test-exchange")
    public Object propertyUpdate(@RequestBody ExchangePayload exchangePayload) {
        EventResponse eventResponse = exchangeFlowExecutor.syncExchangeDown(exchangePayload);
        return ResponseBuilder.success(eventResponse);
    }

    @PostMapping("/public/test-track-flow/{config}")
    public ResponseBody<FlowTraceInfo> testTrackFlow(@PathVariable("config") String config, @RequestBody ExchangePayload exchangePayload) throws IOException {

        ClassPathResource classPathResource = new ClassPathResource("config-schema/"+config + ".json");
        String flowConfig = classPathResource.getContentAsString(Charset.defaultCharset());

        RuleFlowConfig ruleFlowConfig = JsonHelper.fromJSON(flowConfig, RuleFlowConfig.class);
        FlowTraceInfo flowTraceInfo = ruleEngineLifecycleManager.trackFlow(ruleFlowConfig, exchangePayload);

        return ResponseBuilder.success(flowTraceInfo);
    }

    @PostMapping("/public/test-track-node")
    public ResponseBody<NodeTraceInfo> testTrackNode(RuleNodeConfig ruleNodeConfig) throws IOException {

        ExchangePayload exchangePayload = ExchangePayload.create("a.b.c", "test");

        NodeTraceInfo nodeTraceInfo = ruleEngineLifecycleManager.trackNode(ruleNodeConfig, exchangePayload);

        return ResponseBuilder.success(nodeTraceInfo);
    }

    @PostMapping("/public/test-deploy/{config}")
    public ResponseBody<String> testDeploy(@PathVariable("config") String config) throws IOException {

        ClassPathResource classPathResource = new ClassPathResource(config + ".json");
        String flowConfig = classPathResource.getContentAsString(Charset.defaultCharset());

        RuleFlowConfig ruleFlowConfig = JsonHelper.fromJSON(flowConfig, RuleFlowConfig.class);
        String yaml = ruleEngineLifecycleManager.deployFlow(ruleFlowConfig);

        return ResponseBuilder.success(yaml);
    }

}
