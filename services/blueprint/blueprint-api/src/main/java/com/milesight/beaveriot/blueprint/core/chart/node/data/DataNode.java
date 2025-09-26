package com.milesight.beaveriot.blueprint.core.chart.node.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.container.ArrayDataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.container.MapDataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface DataNode extends BlueprintNode {

    @JsonIgnore
    Object getValue();

    @Component
    class Parser implements BlueprintNode.Parser<DataNode> {

        @Lazy
        @Autowired
        DataNode.Parser dataNodeParser;

        @Lazy
        @Autowired
        private List<FunctionNode.Parser<? extends FunctionNode>> functionNodeParsers;

        private Map<String, FunctionNode.Parser<? extends FunctionNode>> functionNameToFunctionNodeParser;

        @Override
        public DataNode parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode, BlueprintParseContext context) {

            if (propertyValue instanceof ObjectNode objectNode) {
                String functionName = null;
                var fieldNames = objectNode.fieldNames();
                while (fieldNames.hasNext()) {
                    var fieldName = fieldNames.next();
                    if (fieldName.startsWith(FunctionNode.PREFIX)) {
                        functionName = fieldName;
                        break;
                    }
                }

                if (functionName != null) {
                    if (functionNameToFunctionNodeParser == null) {
                        functionNameToFunctionNodeParser = functionNodeParsers.stream()
                                .collect(Collectors.toConcurrentMap(
                                        FunctionNode.Parser::getFunctionName, Function.identity(), (a, b) -> a));
                    }

                    var functionNodeParser = functionNameToFunctionNodeParser.get(functionName);
                    if (functionNodeParser == null) {
                        return null;
                    }
                    return functionNodeParser.parse(propertyName, propertyValue, parentNode, context);

                } else {
                    var mapDataNode = new MapDataNode(parentNode, propertyName);
                    BlueprintUtils.forEachInReverseOrder(objectNode.fields(), entry ->
                            context.pushTask(() ->
                                    mapDataNode.addChildNode(dataNodeParser.parse(entry.getKey(), entry.getValue(), mapDataNode, context))));
                    return mapDataNode;
                }
            }

            if (propertyValue instanceof ArrayNode arrayNode) {
                var arrayDataNode = new ArrayDataNode(parentNode, propertyName);
                for (var i = arrayNode.size() - 1; i >= 0; i--) {
                    var item = arrayNode.get(i);
                    var itemName = "[" + i + "]";
                    context.pushTask(() ->
                            arrayDataNode.addChildNode(dataNodeParser.parse(itemName, item, arrayDataNode, context)));
                }
                return arrayDataNode;
            }

            return BlueprintUtils.convertToDataNode(propertyName, parentNode, propertyValue);
        }
    }

}

