package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.exception.RuleEngineException;
import com.milesight.beaveriot.rule.model.trace.FlowTraceResponse;
import com.milesight.beaveriot.rule.model.trace.NodeTraceRequest;
import com.milesight.beaveriot.rule.model.trace.NodeTraceResponse;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;
import org.springframework.util.StringUtils;

/**
 * 放哪里？
 *
 * @author leon
 */
public class DefaultRuleEngineLifecycleManager implements RuleEngineLifecycleManager {

    private DefaultCamelContext camelContext;
    private YamlRoutesBuilderLoader loader;
    private ProducerTemplate producerTemplate;

    public DefaultRuleEngineLifecycleManager(DefaultCamelContext camelContext, ProducerTemplate producerTemplate) {
        this.camelContext = camelContext;
        this.producerTemplate = producerTemplate;
        this.loader = new YamlRoutesBuilderLoader();
        loader.setCamelContext(camelContext);
        loader.build();
    }

    @Override
    public void deployFlow(String flowId, String flowYaml) throws Exception {
        if (!StringUtils.hasText(flowYaml)) {
            throw new RuleEngineException("YAML content is empty: " + flowYaml);
        }

        Resource stringResource = ResourceHelper.fromString(flowId + ".yaml", flowYaml);
        RoutesBuilder routesBuilder = loader.loadRoutesBuilder(stringResource);
        camelContext.addRoutes(routesBuilder);
    }

    @Override
    public void startFlow(String flowId) throws Exception {
        camelContext.startRoute(flowId);
    }

    @Override
    public void stopRoute(String flowId) throws Exception {
        camelContext.stopRoute(flowId);
    }

    @Override
    public void removeFlow(String flowId) throws Exception {
        camelContext.stopRoute(flowId);
        camelContext.removeRoute(flowId);
    }

    @Override
    public boolean validateFlow(String flowYaml) throws Exception {
        if (!StringUtils.hasText(flowYaml)) {
            throw new RuleEngineException("YAML content is empty: " + flowYaml);
        }
        return true;
    }

    @Override
    public FlowTraceResponse trackFlow(String flowId, Exchange exchange) {
        return null;
    }

    @Override
    public NodeTraceResponse trackNode(NodeTraceRequest nodeTraceRequest, Exchange exchange) {
        return null;
    }

}
