package com.milesight.beaveriot.rule.flow.graph;

import com.google.common.graph.MutableGraph;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.*;
import org.apache.camel.impl.engine.DefaultChannel;
import org.apache.camel.spi.AsyncProcessorAwaitManager;
import org.apache.camel.spi.IdAware;
import org.apache.camel.spi.InterceptableProcessor;
import org.apache.camel.spi.RouteIdAware;
import org.apache.camel.support.AsyncProcessorConverterHelper;
import org.apache.camel.support.AsyncProcessorSupport;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.service.ServiceHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * @author leon
 */
@Slf4j
public class GraphProcessor extends AsyncProcessorSupport implements Traceable, IdAware, RouteIdAware, InterceptableProcessor {
    private String id;
    private String routeId;
    private final Map<String, AsyncProcessor> processors;
    private final MutableGraph<String> graphStructure;
    private final String beginNodeId;
    private final CamelContext camelContext;

    public GraphProcessor(CamelContext camelContext, String beginNodeId, Map<String, AsyncProcessor> processors, MutableGraph<String> graphStructure) {
        this.processors = processors;
        this.graphStructure = graphStructure;
        this.beginNodeId = beginNodeId;
        this.camelContext = camelContext;
    }

    @Override
    public boolean process(final Exchange exchange, final AsyncCallback callback) {

        Set<String> successors = graphStructure.successors(beginNodeId);

        GraphTaskExecutor graphTaskExecutor = GraphTaskExecutor.create(successors, graphStructure, processors, camelContext);

        graphTaskExecutor.execute(exchange, callback);

        return true;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public String getTraceLabel() {
        return "graphProcessor";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getRouteId() {
        return routeId;
    }

    @Override
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    @Override
    protected void doInit() throws Exception {
        ServiceHelper.initService(processors.values());
    }

    @Override
    protected void doStart() throws Exception {
        ServiceHelper.startService(processors.values());
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(processors.values());
    }

    @Override
    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownServices(processors.values());
    }

    @Override
    public boolean canIntercept() {
        return false;
    }

    public static abstract class GraphTaskExecutor {

        protected final MutableGraph<String> graphStructure;
        protected final Map<String, AsyncProcessor> processors;
        protected final CamelContext context;

        protected GraphTaskExecutor(MutableGraph<String> graphStructure, Map<String, AsyncProcessor> processors, CamelContext context) {
            this.graphStructure = graphStructure;
            this.processors = processors;
            this.context = context;
        }

        public static GraphTaskExecutor create(Set<String> successors, MutableGraph<String> graphStructure, Map<String, AsyncProcessor> processors, CamelContext context) {
            if (successors.size() == 1) {
                return new SequenceTaskExecutor(successors.iterator().next(), graphStructure, processors, context);
            } else {
                return new ParallelTaskExecutor(successors, graphStructure, processors, context);
            }
        }

        public abstract boolean execute(Exchange exchange, AsyncCallback asyncCallback);

        protected boolean doExecute(String successor, Exchange exchange, AsyncCallback asyncCallback) {

            try {
                Processor processor = processors.get(successor);
                AsyncProcessor asyncProcessor = AsyncProcessorConverterHelper.convert(processor);

                asyncProcessor.process(exchange, asyncCallback);

                Set<String> successors = calculateOutputs(successor, exchange, processor);
                if (CollectionUtils.isEmpty(successors)) {
                    return true;
                }

                if (successors.size() == 1) {
                    String nextSuccessor = successors.iterator().next();
                    doExecute(nextSuccessor, exchange, asyncCallback);
                } else {
                    GraphTaskExecutor graphTaskExecutor = GraphTaskExecutor.create(successors, graphStructure, processors, context);
                    graphTaskExecutor.execute(exchange, asyncCallback);
                }

            } catch (Exception e) {
                exchange.setException(e);
            }
            return true;
        }

        protected Set<String> calculateOutputs(String successor, Exchange exchange, Processor processor) {
            if (processor instanceof GraphChoiceProcessor ||
                    (processor instanceof Channel && ((DefaultChannel) processor).getProcessor() instanceof GraphChoiceProcessor)) {
                String matchId = exchange.getIn().getHeader(ExchangeHeaders.GRAPH_CHOICE_MATCH_ID, String.class);
                return StringUtils.hasText(matchId) ? Set.of(matchId.split(",")) : Set.of();
            } else {
                return graphStructure.successors(successor);
            }
        }
    }

    public static class SequenceTaskExecutor extends GraphTaskExecutor {
        private String successor;

        public SequenceTaskExecutor(String successor, MutableGraph<String> graphStructure, Map<String, AsyncProcessor> processors, CamelContext context) {
            super(graphStructure, processors, context);
            this.successor = successor;
        }

        @Override
        public boolean execute(Exchange exchange, AsyncCallback asyncCallback) {

            asyncCallback.done(true);

            return doExecute(successor, exchange, asyncCallback);
        }
    }

    public static class ParallelTaskExecutor extends GraphTaskExecutor {

        private final Set<String> successors;
        private final AsyncProcessorAwaitManager awaitManager;

        public ParallelTaskExecutor(Set<String> successors, MutableGraph<String> graphStructure, Map<String, AsyncProcessor> processors, CamelContext context) {
            super(graphStructure, processors, context);
            this.successors = successors;
            this.awaitManager = PluginHelper.getAsyncProcessorAwaitManager(context);
        }

        @SneakyThrows
        @Override
        public boolean execute(Exchange exchange, AsyncCallback asyncCallback) {

            try {
                // force synchronous processing using await manager
                awaitManager.process(new AsyncProcessorSupport() {
                    @Override
                    public boolean process(Exchange exchange, AsyncCallback callback) {
                        for (String successor : successors) {
//                            Exchange copy = exchange.copy();
                            doExecute(successor, exchange, callback);
                            //todo parallel process copy exchange, and also restore properties
//                            if (copy.getException() != null) {
//                                exchange.setException(copy.getException());
//                            }
                        }
                        return true;
                    }
                }, exchange);
            } catch (Exception e) {
                exchange.setException(e);
            } finally {
                asyncCallback.done(true);
            }
            return true;
        }

    }
}