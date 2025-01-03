package com.milesight.beaveriot.rule.flow.graph;

import com.google.common.graph.MutableGraph;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.*;
import org.apache.camel.impl.engine.DefaultChannel;
import org.apache.camel.spi.*;
import org.apache.camel.support.AsyncProcessorConverterHelper;
import org.apache.camel.support.AsyncProcessorSupport;
import org.apache.camel.support.ExchangeHelper;
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

        GraphTaskExecutor graphTaskExecutor = GraphTaskExecutor.create(beginNodeId, successors, graphStructure, processors, camelContext);

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

        public static GraphTaskExecutor create(String parentNodeId, Set<String> successors, MutableGraph<String> graphStructure, Map<String, AsyncProcessor> processors, CamelContext context) {
            if (successors.size() == 1) {
                return new SequenceTaskExecutor(successors.iterator().next(), graphStructure, processors, context);
            } else {
                return new ParallelTaskExecutor(parentNodeId, successors, graphStructure, processors, context);
            }
        }

        public abstract boolean execute(Exchange exchange, AsyncCallback asyncCallback);

        protected boolean doExecute(String successor, Exchange exchange, AsyncCallback asyncCallback) {

            try {
                if (exchange.getException() != null) {
                    return true;
                }

                Processor processor = processors.get(successor);
                AsyncProcessor asyncProcessor = AsyncProcessorConverterHelper.convert(processor);

                asyncProcessor.process(exchange, asyncCallback);

                Set<String> successors = calculateOutputs(successor, exchange, processor);
                if (CollectionUtils.isEmpty(successors)) {
                    return true;
                }

                if (successors.size() == 1) {
                    String nextSuccessor = successors.iterator().next();
                    setExchangeTraceId(exchange, exchange.getExchangeId(), successor);
                    doExecute(nextSuccessor, exchange, asyncCallback);
                } else {
                    GraphTaskExecutor graphTaskExecutor = GraphTaskExecutor.create(successor, successors, graphStructure, processors, context);
                    graphTaskExecutor.execute(exchange, asyncCallback);
                }

            } catch (Exception e) {
                catchExceptionIfNecessary(exchange, e);
                throw e;
            }
            return true;
        }

        protected void setExchangeTraceId(Exchange exchange, String parentExchangeId, String parentNodeId) {
            String traceId = parentExchangeId + "-" + parentNodeId;
            exchange.getIn().setHeader(ExchangeHeaders.EXCHANGE_LATEST_TRACE_ID, traceId);
        }


        protected void catchExceptionIfNecessary(Exchange exchange, Exception e) {
            if (exchange.getException() == null) {
                exchange.setException(e);
            }
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

            setExchangeTraceId(exchange, exchange.getExchangeId(), successor);

            return doExecute(successor, exchange, asyncCallback);
        }
    }

    public static class ParallelTaskExecutor extends GraphTaskExecutor {

        private final String parentNodeId;
        private final Set<String> successors;
        private final AsyncProcessorAwaitManager awaitManager;


        public ParallelTaskExecutor(String parentNodeId, Set<String> successors, MutableGraph<String> graphStructure, Map<String, AsyncProcessor> processors, CamelContext context) {
            super(graphStructure, processors, context);
            this.successors = successors;
            this.parentNodeId = parentNodeId;
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
                        Exception caughtException = null;
                        for (String successor : successors) {
                            Exchange copyExchange = exchange.copy();
                            copyExchange.getIn().setMessageId(null);
                            try {
                                setExchangeTraceId(copyExchange, exchange.getExchangeId(), parentNodeId);
                                doExecute(successor, copyExchange, callback);
                            } catch (Exception ex) {
                                catchExceptionIfNecessary(copyExchange, ex);
                            } finally {
                                if (copyExchange.getException() != null) {
                                    caughtException = copyExchange.getException();
                                }
                                copyPropertiesResult(exchange, copyExchange);
                            }
                        }

                        if (caughtException != null) {
                            exchange.setException(caughtException);
                        }

                        return true;
                    }
                    private void copyPropertiesResult(Exchange target, Exchange source) {
                        if (source.hasProperties()) {
                            target.getProperties().putAll(source.getProperties());
                        }

                        final ExchangeExtension sourceExtension = source.getExchangeExtension();
                        sourceExtension.copyInternalProperties(target);

                        final ExchangeExtension resultExtension = target.getExchangeExtension();
                        sourceExtension.copySafeCopyPropertiesTo(resultExtension);
                    }

                }, exchange);
            } catch (Exception e) {
                catchExceptionIfNecessary(exchange, e);
            } finally {
                asyncCallback.done(true);
            }
            return true;
        }

    }
}