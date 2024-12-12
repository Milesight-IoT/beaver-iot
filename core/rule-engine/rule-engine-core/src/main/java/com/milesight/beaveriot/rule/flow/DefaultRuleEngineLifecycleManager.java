package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.exception.RuleEngineException;
import com.milesight.beaveriot.rule.flow.builder.RuleFlowYamlBuilder;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.trace.FlowTraceResponse;
import com.milesight.beaveriot.rule.model.trace.NodeTraceResponse;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
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
        Assert.notNull(ruleFlowConfig.getFlowId(), "Rule flow id must not be null");

        String dumpYaml = RuleFlowYamlBuilder.builder()
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
    public void removeFlow(String flowId) {
        try {
            camelContext.stopRoute(flowId);
            camelContext.removeRoute(flowId);
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
    public FlowTraceResponse trackFlow(RuleFlowConfig ruleFlowConfig, Exchange exchange) {

        final String flowId = RuleFlowIdGenerator.generateRandomId();

        ruleFlowConfig.setFlowId(flowId);

        exchange.setProperty(ExchangeHeaders.TRACE_FOR_TEST, true);

        return executeWithRollback(ruleFlowConfig, () -> {
            try {
                AdviceWith.adviceWith(flowId, camelContext, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        this.replaceFromWith("direct:" + flowId);
                    }
                });
            } catch (Exception e) {
                throw new RuleEngineException("Trace Rule Flow Exception", e);
            }
            String endpointUri = camelContext.getRoute(flowId).getEndpoint().getEndpointUri();
            ruleEngineExecutor.execute(endpointUri, exchange);
            return exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE, FlowTraceResponse.class);
        });
    }

    @Override
    public FlowTraceResponse trackFlow(RuleFlowConfig ruleFlowConfig, Object body) {
        DefaultExchange defaultExchange = new DefaultExchange(camelContext);
        defaultExchange.getIn().setBody(body);
        return trackFlow(ruleFlowConfig, defaultExchange);
    }

    @Override
    public NodeTraceResponse trackNode(RuleNodeConfig nodeConfig, Exchange exchange) {
        RuleNodeConfig fromNode = RuleNodeConfig.create(RuleFlowIdGenerator.generateRandomId(), RuleNodeNames.CAMEL_DIRECT, "TestMockNode", null);
        RuleFlowConfig sequenceFlow = RuleFlowConfig.createSequenceFlow(RuleFlowIdGenerator.generateRandomId(), List.of(fromNode, nodeConfig));
        FlowTraceResponse flowTraceResponse = trackFlow(sequenceFlow, exchange);
        return flowTraceResponse.getLastNodeTrace();
    }

    @Override
    public NodeTraceResponse trackNode(RuleNodeConfig nodeConfig, Object body) {
        DefaultExchange defaultExchange = new DefaultExchange(camelContext);
        defaultExchange.getIn().setBody(body);
        return trackNode(nodeConfig, defaultExchange);
    }

    private <T> T executeWithRollback(RuleFlowConfig ruleFlowConfig, Supplier<T> supplier) {

        String routeYaml = null;
        try {
            routeYaml = deployFlow(ruleFlowConfig);
            return supplier.get();
        } catch (Exception ex) {
            log.error("Execute route withRollback exception, Yaml content is : {}", routeYaml, ex);
            throw new RuleEngineException("Execute route withRollback exception", ex);
        } finally {
            removeFlow(ruleFlowConfig.getFlowId());
        }
    }

}
