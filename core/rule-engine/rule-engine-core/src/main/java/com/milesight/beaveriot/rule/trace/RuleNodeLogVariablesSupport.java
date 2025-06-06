package com.milesight.beaveriot.rule.trace;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.model.VariableNamed;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.milesight.beaveriot.rule.constants.ExchangeHeaders.EXCHANGE_CUSTOM_INPUT_LOG_VARIABLES;
import static com.milesight.beaveriot.rule.constants.ExchangeHeaders.EXCHANGE_CUSTOM_OUTPUT_LOG_VARIABLES;

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
        if (ExchangeHeaders.containsMapProperty(exchange, EXCHANGE_CUSTOM_INPUT_LOG_VARIABLES, nodeId)) {
            return toJSON(ExchangeHeaders.getMapProperty(exchange, EXCHANGE_CUSTOM_INPUT_LOG_VARIABLES, nodeId));
        }

        try {
            LogVariables logVariables = getLogVariables(exchange.getFromRouteId(), nodeId);
            if (logVariables == null || ObjectUtils.isEmpty(logVariables.getInputVariables())) {
                return null;
            }
            List<SpelNode> spelNodeList = new ArrayList<>();
            for (String inputVariable : logVariables.getInputVariables()) {
                // parser SpEL variable ,like : properties.node_xxx['abc']， properties['node_xxx']['abc'], properties['node_xxx']['abc']==properties['node_xxx']['abc2'] and so on..
                for (SpelExpression spelExpression : SpELExpressionHelper.extractSpELExpression(inputVariable)) {
                    visitPropertiesExpression(spelExpression.getAST(), spelNodeList);
                }
            }
            Map<String, Object> inputVariables = new LinkedHashMap<>();
            for (SpelNode spelNode : spelNodeList) {
                SpelNode spelNodeChild = spelNode.getChild(spelNode.getChildCount() - 1);
                if (spelNodeChild instanceof PropertyOrFieldReference || spelNodeChild instanceof Indexer) {
                    String variableNameOri = spelNodeChild instanceof Indexer indexer ? indexer.getChild(0).toStringAST() : spelNodeChild.toStringAST();
                    String variableName = variableNameOri.replaceAll("'", "").replaceAll("\"", ""); // remove quotes
                    inputVariables.put(variableName, SpELExpressionHelper.SPEL_EXPRESSION_PREFIX + spelNode.toStringAST() + SpELExpressionHelper.SPEL_EXPRESSION_SUFFIX);
                }else {
                    log.warn("Unsupported SpEL node type: {}", spelNodeChild.getClass().getSimpleName());
                }
            }
            return toJSON(SpELExpressionHelper.resolveExpression(exchange, inputVariables));
        } catch (Exception ex) {
            return  causeException(ex);
        }
    }

    private static void visitPropertiesExpression(SpelNode spelNode, List<SpelNode> spelNodeList) {
        boolean isPropertiesExpression = isPropertiesExpression(spelNode);
        if (isPropertiesExpression) {
            spelNodeList.add(spelNode);
            return;
        }

        for (int i = 0; i < spelNode.getChildCount(); i++) {
            SpelNode child = spelNode.getChild(i);
            visitPropertiesExpression(child, spelNodeList);
        }
    }

    private static boolean isPropertiesExpression(SpelNode spelNode) {
        return spelNode instanceof CompoundExpression && spelNode.getChildCount() >= 3 &&
                spelNode.getChild(0) instanceof PropertyOrFieldReference &&
                ((PropertyOrFieldReference)spelNode.getChild(0)).getName().equals("properties");
    }


    public static String getExchangeOutputBody(Exchange exchange, String nodeId) {
        if (ExchangeHeaders.containsMapProperty(exchange, EXCHANGE_CUSTOM_OUTPUT_LOG_VARIABLES, nodeId)) {
            return toJSON(ExchangeHeaders.getMapProperty(exchange, EXCHANGE_CUSTOM_OUTPUT_LOG_VARIABLES, nodeId));
        }

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
