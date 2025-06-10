package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.RuleNodeDefinitionInterceptor;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.exception.RuleEngineException;
import com.milesight.beaveriot.rule.flow.graph.DefaultRuleNodeDefinitionInterceptor;
import com.milesight.beaveriot.rule.flow.graph.GraphRouteDefinitionGenerator;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.flow.route.FromNodeDefinition;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import com.milesight.beaveriot.rule.trace.RuleNodeLogVariablesSupport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.Model;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.ResourceHelper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

import static com.milesight.beaveriot.rule.constants.ExchangeHeaders.EXCHANGE_CUSTOM_OUTPUT_LOG_VARIABLES;

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

    @SneakyThrows
    @Override
    public void deployFlow(RuleFlowConfig ruleFlowConfig) {
        deployFlow(ruleFlowConfig, null);
    }

    @SneakyThrows
    private void deployFlow(RuleFlowConfig ruleFlowConfig, RuleNodeDefinitionInterceptor ruleNodeDefinitionInterceptor) {
        Assert.notNull(ruleFlowConfig.getFlowId(), "Rule flow id must not be null");

        camelContext.getCamelContextExtension().getContextPlugin(Model.class)
                .addRouteDefinitions(GraphRouteDefinitionGenerator.generateRouteDefinition(ruleFlowConfig, ruleNodeDefinitionInterceptor));
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
            RuleNodeLogVariablesSupport.removeLogVariables(flowId);
            camelContext.stopRoute(flowId);
            return camelContext.removeRoute(flowId);
        } catch (Exception e) {
            throw new RuleEngineException("Remove Flow Exception:", e);
        }
    }

    @Override
    public boolean removeFlowImmediately(String flowId) {
        try {
            return camelContext.removeRoute(flowId);
        } catch (Exception e) {
            throw new RuleEngineException("Remove Flow Exception:", e);
        }
    }

    @Override
    public boolean validateFlow(RuleFlowConfig ruleFlowConfig) {
        ruleFlowConfig.setFlowId(RuleFlowIdGenerator.generateRandomId());
        try {
            return executeWithRollback(ruleFlowConfig, new TraceFlowRuleNodeDefinitionInterceptor(), () -> true);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public FlowTraceInfo trackFlow(RuleFlowConfig ruleFlowConfig, Exchange exchange) {
        return trackFlow(ruleFlowConfig, exchange, new TraceFlowRuleNodeDefinitionInterceptor());
    }

    private FlowTraceInfo trackFlow(RuleFlowConfig ruleFlowConfig, Exchange exchange, RuleNodeDefinitionInterceptor ruleNodeDefinitionInterceptor) {

        exchange.setProperty(ExchangeHeaders.TRACE_FOR_TEST, true);
        exchange.setProperty(ExchangeHeaders.TRACE_RESPONSE, FlowTraceInfo.create(ruleFlowConfig.getFlowId()));

        final String newFlowId = RuleFlowIdGenerator.generateRandomId();
        ruleFlowConfig.setFlowId(newFlowId);

        return executeWithRollback(ruleFlowConfig, ruleNodeDefinitionInterceptor, () -> {
            String endpointUri = camelContext.getRoute(newFlowId).getEndpoint().getEndpointUri();
            ExchangeHeaders.putMapProperty(exchange, EXCHANGE_CUSTOM_OUTPUT_LOG_VARIABLES, ruleFlowConfig.getFromNodeId(), exchange.getIn().getBody());
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
        return flowTraceResponse.findLastNodeTrace();
    }

    @Override
    public NodeTraceInfo trackNode(RuleNodeConfig nodeConfig, Object body) {
        DefaultExchange defaultExchange = new DefaultExchange(camelContext);
        defaultExchange.getIn().setBody(body);
        return trackNode(nodeConfig, defaultExchange);
    }

    private <T> T executeWithRollback(RuleFlowConfig ruleFlowConfig, RuleNodeDefinitionInterceptor ruleNodeDefinitionInterceptor, Supplier<T> supplier) {

        try {
            deployFlow(ruleFlowConfig, ruleNodeDefinitionInterceptor);
            return supplier.get();
        } catch (Exception ex) {
            log.error("Test execution route failed, design json is : {}", ruleFlowConfig, ex);
            throw new RuleEngineException("Test execution route failed", ex);
        } finally {
            removeFlow(ruleFlowConfig.getFlowId());
        }
    }

    public class TraceFlowRuleNodeDefinitionInterceptor extends DefaultRuleNodeDefinitionInterceptor {
        @Override
        public FromNodeDefinition interceptFromNodeDefinition(String flowId, FromNodeDefinition fromNode) {
            return FromNodeDefinition.create(fromNode.getId(), fromNode.getNameNode(), "direct:" + flowId, null);
        }
    }

}
