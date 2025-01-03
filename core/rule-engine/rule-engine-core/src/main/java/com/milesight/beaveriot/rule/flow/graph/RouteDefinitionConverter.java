package com.milesight.beaveriot.rule.flow.graph;

import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.route.*;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import org.apache.camel.Expression;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.WhenDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author leon
 */
public class RouteDefinitionConverter {

    private RouteDefinitionConverter() {
    }

    public static FromDefinition convertFromDefinition(String flowId, FromNodeDefinition nodeDefinition) {
        FromDefinition fromDefinition = new FromDefinition();
        fromDefinition.setId(RuleFlowIdGenerator.generateNamespacedId(flowId, nodeDefinition.getId()));
        fromDefinition.setUri(generateUri(nodeDefinition.getUri(), nodeDefinition.getParameters()));
        fromDefinition.setDescription(nodeDefinition.getNameNode());
        return fromDefinition;
    }

    public static ProcessorDefinition<?> convertProcessorDefinition(String flowId, AbstractNodeDefinition nodeDefinition, Map<String, List<String>> choiceWhenEdges) {
        String namespacedId = RuleFlowIdGenerator.generateNamespacedId(flowId, nodeDefinition.getId());
        if (nodeDefinition instanceof ToNodeDefinition toNodeDefinition) {
            ToDefinition toDefinition = new ToDefinition();
            toDefinition.setUri(generateUri(toNodeDefinition.getUri(), toNodeDefinition.getParameters()));
            toDefinition.setId(namespacedId);
            toDefinition.setDescription(toNodeDefinition.getNameNode());
            return toDefinition;
        } else if (nodeDefinition instanceof ChoiceNodeDefinition choiceNodeDefinition) {
            GraphChoiceDefinition choiceDefinition = new GraphChoiceDefinition();
            choiceDefinition.setId(namespacedId);
            Map<String, WhenDefinition> whenClause = choiceNodeDefinition.getWhenNodeDefinitions()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> getSuccessors(entry.getKey(), choiceWhenEdges), entry -> convertWhenDefinition(flowId, entry.getValue())));
            choiceDefinition.setWhenClause(whenClause);
            choiceDefinition.setOtherwiseNodeId(getSuccessors(choiceNodeDefinition.getOtherwiseNodeId(), choiceWhenEdges));
            return choiceDefinition;
        } else {
            throw new IllegalArgumentException("Unsupported node type: " + nodeDefinition.getClass().getName());
        }
    }

    public static WhenDefinition convertWhenDefinition(String flowId, WhenNodeDefinition nodeDefinition) {
        String namespacedId = RuleFlowIdGenerator.generateNamespacedId(flowId, nodeDefinition.getId());
        WhenDefinition whenDefinition = new WhenDefinition();
        whenDefinition.setId(namespacedId);

        ExpressionNode expressionNode = nodeDefinition.getExpression();
        Expression expression = ExpressionBuilder.languageExpression(expressionNode.getLanguage(), expressionNode.getExpression());
        ExpressionDefinition exp = new ExpressionDefinition(expression);
        whenDefinition.setExpression(exp);
        return whenDefinition;
    }

    private static String getSuccessors(String key, Map<String, List<String>> choiceWhenEdges) {
        List<String> successors = choiceWhenEdges.get(key);
        return ObjectUtils.isEmpty(successors) ? null : StringUtils.join(successors, ",");
    }

    private static String generateUri(String uri, Map<String, Object> parameters) {
        if (!ObjectUtils.isEmpty(parameters)) {
            return uri + "?" + parameters.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));
        } else {
            return uri;
        }
    }
}
