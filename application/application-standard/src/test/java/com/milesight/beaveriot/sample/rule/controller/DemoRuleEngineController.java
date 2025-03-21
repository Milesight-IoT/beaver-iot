package com.milesight.beaveriot.sample.rule.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.security.SecurityUser;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.rule.RuleEngineComponentManager;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.sample.entity.DemoIntegrationEntities;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author leon
 */
@Slf4j
@RestController
public class DemoRuleEngineController {

    @Autowired
    private RuleEngineLifecycleManager ruleEngineLifecycleManager;
    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;
    @Autowired
    private RuleEngineComponentManager ruleEngineComponentManager;
    @Autowired
    private RuleEngineExecutor ruleEngineExecutor;

    @PostMapping("/public/exec/{directName}")
    public String exec(@PathVariable("directName") String name, @RequestBody ExchangePayload exchangePayload) {
        ruleEngineExecutor.execute("direct:" + name, exchangePayload);
        return "success";
    }


    @GetMapping("/public/schema/{name}")
    public String schema(@PathVariable("name") String name) {
        return ruleEngineComponentManager.getComponentDefinitionSchema(name);
    }

    @PostMapping("/public/test-exchange")
    public Object propertyUpdate(@RequestBody ExchangePayload exchangePayload) {
        SecurityUser securityUser = SecurityUser.builder().userId(11111111L).tenantId("default").build();
        SecurityUserContext.setSecurityUser(securityUser);
        entityValueServiceProvider.saveValuesAndPublishSync(exchangePayload);
        return ResponseBuilder.success("eventResponse");
    }

    @PostMapping("/public/test-exchange-wrapper")
    public Object propertyUpdateWrapper(@RequestBody ExchangePayload exchangePayload) {
        SecurityUser securityUser = SecurityUser.builder().userId(11111111L).build();
        SecurityUserContext.setSecurityUser(securityUser);

        AnnotatedEntityWrapper<DemoIntegrationEntities.DemoGroupSettingEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValues(Map.of(DemoIntegrationEntities.DemoGroupSettingEntities::getLength,100,
                            DemoIntegrationEntities.DemoGroupSettingEntities::getAccessKey, "200",
                            DemoIntegrationEntities.DemoGroupSettingEntities::getSecretKey, "300"))
                .publishAsync();

        return ResponseBuilder.success("eventResponse");
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
    public ResponseBody<NodeTraceInfo> testTrackNode(@RequestBody String ruleNodeConfigStr) throws IOException {

        RuleNodeConfig ruleNodeConfig = JsonHelper.fromJSON(ruleNodeConfigStr, RuleNodeConfig.class);
        ExchangePayload exchangePayload = ExchangePayload.create("demo-anno-integration.integration.connect", "test");

        NodeTraceInfo nodeTraceInfo = ruleEngineLifecycleManager.trackNode(ruleNodeConfig, exchangePayload);

        return ResponseBuilder.success(nodeTraceInfo);
    }

    @PostMapping("/public/test-deploy/{config}")
    public String testDeploy(@PathVariable("config") String config) throws IOException {

        ClassPathResource classPathResource = new ClassPathResource("config-schema/"+config + ".json");
        String flowConfig = classPathResource.getContentAsString(Charset.defaultCharset());

        RuleFlowConfig ruleFlowConfig = JsonHelper.fromJSON(flowConfig, RuleFlowConfig.class);
        ruleEngineLifecycleManager.deployFlow(ruleFlowConfig);

        return "success";
    }

}
