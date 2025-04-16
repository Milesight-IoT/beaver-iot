package com.milesight.beaveriot.rule.flow.graph;

import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Channel;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteDefinitionHelper;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.spi.IdAware;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.RouteIdAware;
import org.apache.camel.support.PluginHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
public class GraphProcessorReifier extends ProcessorReifier<GraphProcessorDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(GraphProcessorReifier.class);

    public GraphProcessorReifier(Route route, ProcessorDefinition<?> definition) {
        super(route, GraphProcessorDefinition.class.cast(definition));
    }

    @Override
    public Processor createProcessor() throws Exception {

        Map<String, AsyncProcessor> processors = new LinkedHashMap<>();
        FlowGraph flowGraph = definition.getFlowGraph();
        flowGraph.getNodeDefinitions().entrySet().forEach(entry -> {
            AsyncProcessor processor = createChannel(entry.getValue());
            processors.put(entry.getKey(), processor);
        });

        String originFromId = RuleFlowIdGenerator.removeNamespacedId(flowGraph.getFlowId(), flowGraph.getFromDefinition().getId());

        return new GraphProcessor(getCamelContext(), originFromId, processors, flowGraph.getGraphStructure(), flowGraph.outputNodeId);
    }

    protected AsyncProcessor createChannel(ProcessorDefinition<?> value) {
        try {
            Processor processor = createProcessor(value);
            // inject id
            if (processor instanceof IdAware idAware) {
                String id = getId(definition);
                idAware.setId(id);
            }
            if (processor instanceof RouteIdAware routeIdAware) {
                routeIdAware.setRouteId(route.getRouteId());
            }

            if (processor == null) {
                // no processor to make
                return null;
            }
            return wrapChannel(processor, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create processor for " + value, e);
        }
    }

    @Override
    protected Channel wrapChannel(Processor processor, ProcessorDefinition<?> child, Boolean inheritErrorHandler)
            throws Exception {
        // put a channel in between this and each output to control the route flow logic
        Channel channel = PluginHelper.getInternalProcessorFactory(camelContext)
                .createChannel(camelContext);

        // add interceptor strategies to the channel must be in this order:
        // camel context, route context, local
        List<InterceptStrategy> interceptors = new ArrayList<>();
        interceptors.addAll(camelContext.getCamelContextExtension().getInterceptStrategies());
        interceptors.addAll(route.getInterceptStrategies());
        interceptors.addAll(definition.getInterceptStrategies());

        // force the creation of an id
        RouteDefinitionHelper.forceAssignIds(camelContext, definition);

        // fix parent/child relationship. This will be the case of the routes
        if (child != null && definition != child) {
            child.setParent(definition);
        }

        // set the child before init the channel
        RouteDefinition route = ProcessorDefinitionHelper.getRoute(definition);
        boolean first = false;
        if (route != null && !route.getOutputs().isEmpty()) {
            first = route.getOutputs().get(0) == definition;
        }
        // initialize the channel
        channel.initChannel(this.route, definition, child, interceptors, processor, route, first);

        // todo: remove error handler from the channel and set it to the route
        // do post init at the end
        channel.postInitChannel();
        LOG.trace("{} wrapped in Channel: {}", definition, channel);

        return channel;
    }
}
