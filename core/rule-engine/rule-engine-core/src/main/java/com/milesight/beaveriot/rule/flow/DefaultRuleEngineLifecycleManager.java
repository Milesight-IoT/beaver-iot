package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.RuleNodeInterceptor;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.exception.RuleEngineException;
import com.milesight.beaveriot.rule.flow.builder.DefaultRuleNodeInterceptor;
import com.milesight.beaveriot.rule.flow.builder.RuleFlowYamlBuilder;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.flow.route.FromNode;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.ResourceHelper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author leon
 */
@Slf4j
public class DefaultRuleEngineLifecycleManager implements RuleEngineLifecycleManager {

    private DefaultCamelContext camelContext;
    private YamlRoutesBuilderLoader loader;
    private RuleEngineExecutor ruleEngineExecutor;

    public DefaultRuleEngineLifecycleManager(CamelContext context, RuleEngineExecutor ruleEngineExecutor) {
        this.loader = new YamlRoutesBuilderLoader();
        loader.setCamelContext(context);
        loader.build();
        this.camelContext = (DefaultCamelContext) context;
        this.ruleEngineExecutor = ruleEngineExecutor;
    }

    @Override
    public String deployFlow(RuleFlowConfig ruleFlowConfig) {
        return deployFlow(ruleFlowConfig, null);
    }

    private String deployFlow(RuleFlowConfig ruleFlowConfig, RuleNodeInterceptor ruleNodeInterceptor) {
        Assert.notNull(ruleFlowConfig.getFlowId(), "Rule flow id must not be null");

        String dumpYaml = RuleFlowYamlBuilder.builder(ComponentDefinitionCache::load, ruleNodeInterceptor)
                .withRuleFlowConfig(ruleFlowConfig)
                .build()
                .dumpYaml();
        deployFlow(ruleFlowConfig.getFlowId(), dumpYaml);
        return dumpYaml;
    }

    @Override
    public void deployFlow(String flowId, String flowRouteYaml) {
        if (!StringUtils.hasText(flowRouteYaml)) {
            throw new RuleEngineException("YAML content is empty: " + flowRouteYaml);
        }

        try {
            Resource stringResource = ResourceHelper.fromString(flowId + ".yaml", flowRouteYaml);
            RoutesBuilder routesBuilder = loader.loadRoutesBuilder(stringResource);
            camelContext.addRoutes(routesBuilder);
        } catch (Exception e) {
            throw new RuleEngineException("Deploy Flow Exception:", e);
        }
    }

    @Override
    public void startRoute(String flowId) {
        try {
            camelContext.startRoute(flowId);
        } catch (Exception e) {
            throw new RuleEngineException("Start Route Exception:", e);
        }
    }

    @Override
    public void stopRoute(String flowId) {
        try {
            camelContext.stopRoute(flowId);
        } catch (Exception e) {
            throw new RuleEngineException("Stop Route Exception:", e);
        }
    }

    @Override
    public boolean removeFlow(String flowId) {
        try {
            camelContext.stopRoute(flowId);
            return camelContext.removeRoute(flowId);
        } catch (Exception e) {
            throw new RuleEngineException("Remove Flow Exception:", e);
        }
    }

    @Override
    public boolean validateFlow(RuleFlowConfig ruleFlowConfig) {
        ruleFlowConfig.setFlowId(RuleFlowIdGenerator.generateRandomId());
        try {
            return executeWithRollback(ruleFlowConfig, () -> true);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public FlowTraceInfo trackFlow(RuleFlowConfig ruleFlowConfig, Exchange exchange) {

        exchange.setProperty(ExchangeHeaders.TRACE_FOR_TEST, true);
        exchange.setProperty(ExchangeHeaders.TRACE_RESPONSE, FlowTraceInfo.create(ruleFlowConfig.getFlowId()));

        final String newFlowId = RuleFlowIdGenerator.generateRandomId();
        ruleFlowConfig.setFlowId(newFlowId);

        return executeWithRollback(ruleFlowConfig, () -> {
            String endpointUri = camelContext.getRoute(newFlowId).getEndpoint().getEndpointUri();
            ruleEngineExecutor.execute(endpointUri, exchange);
            return exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE, FlowTraceInfo.class);
        });
    }

    @Override
    public FlowTraceInfo trackFlow(RuleFlowConfig ruleFlowConfig, Object body) {
        DefaultExchange defaultExchange = new DefaultExchange(camelContext);
        defaultExchange.getIn().setBody(body);
        return trackFlow(ruleFlowConfig, defaultExchange);
    }

    @Override
    public NodeTraceInfo trackNode(RuleNodeConfig nodeConfig, Exchange exchange) {
        RuleNodeConfig fromNode = RuleNodeConfig.create(RuleFlowIdGenerator.generateRandomId(), RuleNodeNames.CAMEL_DIRECT, "TestMockNode", null);
        RuleFlowConfig sequenceFlow = RuleFlowConfig.createSequenceFlow(RuleFlowIdGenerator.generateRandomId(), List.of(fromNode, nodeConfig));
        FlowTraceInfo flowTraceResponse = trackFlow(sequenceFlow, exchange);
        return flowTraceResponse.getLastNodeTrace();
    }

    @Override
    public NodeTraceInfo trackNode(RuleNodeConfig nodeConfig, Object body) {
        DefaultExchange defaultExchange = new DefaultExchange(camelContext);
        defaultExchange.getIn().setBody(body);
        return trackNode(nodeConfig, defaultExchange);
    }

    private <T> T executeWithRollback(RuleFlowConfig ruleFlowConfig, Supplier<T> supplier) {

        String routeYaml = null;
        try {
            routeYaml = deployFlow(ruleFlowConfig, new TraceRuleNodeInterceptor());
            return supplier.get();
        } catch (Exception ex) {
            log.error("Execute route withRollback exception, Yaml content is : {}", routeYaml, ex);
            throw new RuleEngineException("Execute route withRollback exception", ex);
        } finally {
            removeFlow(ruleFlowConfig.getFlowId());
        }
    }

    public static class TraceRuleNodeInterceptor extends DefaultRuleNodeInterceptor {

        @Override
        public FromNode interceptFromNode(String flowId, FromNode fromNode) {
            return new FromNode(fromNode.getId(), "direct:" + flowId, null, fromNode.getSteps());
        }
    }
}
