package com.milesight.beaveriot.rule.trace;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.rule.model.VariableNamed;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leon
 */
@Slf4j
public class RuleNodeLogVariablesSupport {

    private static final long TRACER_BODY_MAX_LENGTH = 100000;
    private static final String TRACER_BODY_EXCEED_MAX_WARNING = "Tracer body exceed maximum length " + TRACER_BODY_MAX_LENGTH;
    private static final Map<String, Map<String, LogVariables>> RULE_NODE_LOG_VARIABLES_CACHE = new ConcurrentHashMap<>();

    private RuleNodeLogVariablesSupport() {
    }

    public static Map<String, LogVariables> cacheInputLogVariables(String flowId, String ruleNodeId, Set<String> parameter) {
        Map<String, LogVariables> variablesMap = RULE_NODE_LOG_VARIABLES_CACHE.computeIfAbsent(flowId, key -> Maps.newHashMap());
        LogVariables logVariables = variablesMap.containsKey(ruleNodeId) ? variablesMap.get(ruleNodeId) : new LogVariables();
        logVariables.setInputVariables(parameter);
        variablesMap.put(ruleNodeId, logVariables);
        return variablesMap;
    }

    public static Map<String, LogVariables> cacheOutputLogVariables(String flowId, String ruleNodeId, List<VariableNamed> outputKeys) {
        Map<String, LogVariables> variablesMap = RULE_NODE_LOG_VARIABLES_CACHE.computeIfAbsent(flowId, key -> Maps.newHashMap());
        LogVariables logVariables = variablesMap.containsKey(ruleNodeId) ? variablesMap.get(ruleNodeId) : new LogVariables();
        logVariables.setOutputVariables(outputKeys);
        variablesMap.put(ruleNodeId, logVariables);
        return variablesMap;
    }

    public static String getExchangeInputBody(Exchange exchange, String nodeId) {
        try {
            LogVariables logVariables = getLogVariables(exchange.getFromRouteId(), nodeId);
            if (logVariables == null || ObjectUtils.isEmpty(logVariables.getInputVariables())) {
                return null;
            }
            Map<String, Object> inputVariables = new LinkedHashMap<>();
            for (String inputVariable : logVariables.getInputVariables()) {
                //fixme:  parser SpEL variable ,like : properties.node_xxx['abc']
                String variableNodeId = StringUtils.substringBetween(inputVariable, "properties.","[");
                String variableName = StringUtils.substringBetween(inputVariable, "['", "']");
                if (StringUtils.isEmpty(variableNodeId) || StringUtils.isEmpty(variableName)) {
                    continue;
                }
                LogVariables variableNode = getLogVariables(exchange.getFromRouteId(), variableNodeId);
                if (variableNode != null) {
                    variableNode.findOutputVariable(variableName).ifPresent(definitionNamed -> inputVariables.put(definitionNamed.getName(), SpELExpressionHelper.SPEL_EXPRESSION_PREFIX + inputVariable + SpELExpressionHelper.SPEL_EXPRESSION_SUFFIX));
                }
            }
            String bodyStr = JsonUtils.toJSON(SpELExpressionHelper.resolveExpression(exchange, inputVariables));
            return !ObjectUtils.isEmpty(bodyStr) && bodyStr.length() > TRACER_BODY_MAX_LENGTH ? TRACER_BODY_EXCEED_MAX_WARNING : bodyStr;
        } catch (Exception ex) {
            return  causeException(ex);
        }
    }

    public static String getExchangeOutputBody(Exchange exchange, String nodeId) {
        try {
            LogVariables logVariables = getLogVariables(exchange.getFromRouteId(), nodeId);
            if (logVariables == null || ObjectUtils.isEmpty(logVariables.getOutputVariables())) {
                return null;
            }
            Object body = exchange.getIn().getBody();
            if (body instanceof Map) {
                Map<String, Object> extractBody = new HashMap<>();
                for (Map.Entry<String, ?> entry : ((Map<String, ?>) body).entrySet()) {
                    Optional<VariableNamed> definitionNamedOptional = logVariables.findOutputVariable(entry.getKey());
                    definitionNamedOptional.ifPresent(definitionNamed -> extractBody.put(definitionNamed.getName(), entry.getValue()));
                }
                return toJSON(extractBody);
            } else {
                return toJSON(body);
            }
        } catch (Exception ex) {
            return causeException(ex);
        }
    }

    protected static String toJSON(Object extractBody) {
        try {
            if (extractBody == null) {
                return null;
            }
            String bodyStr = JsonUtils.toJSON(extractBody);
            return !ObjectUtils.isEmpty(bodyStr) && bodyStr.length() > TRACER_BODY_MAX_LENGTH ? TRACER_BODY_EXCEED_MAX_WARNING : bodyStr;
        } catch (Exception ex) {
            return causeException(ex);
        }
    }

    protected static LogVariables getLogVariables(String flowId, String nodeId) {
        if (!RULE_NODE_LOG_VARIABLES_CACHE.containsKey(flowId)) {
            return null;
        }
        Map<String, LogVariables> logVariablesMap = RULE_NODE_LOG_VARIABLES_CACHE.get(flowId);
        return logVariablesMap.containsKey(nodeId) ? logVariablesMap.get(nodeId) : null;
    }

    public static void removeLogVariables(String flowId) {
        RULE_NODE_LOG_VARIABLES_CACHE.remove(flowId);
    }

    private static String causeException(Exception ex) {
        log.error("Convert exchange body failed on tracing", ex);
        return "Convert exchange body failed:" + ex.getMessage();
    }

    @Data
    public static class LogVariables {

        private Set<String> inputVariables;
        private List<VariableNamed> outputVariables;

        public Optional<VariableNamed> findOutputVariable(String name) {
            if (ObjectUtils.isEmpty(outputVariables)) {
                return Optional.empty();
            }
            return outputVariables.stream().filter(item -> item.match(name)).findFirst();
        }
    }
}
