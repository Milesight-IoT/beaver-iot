package com.milesight.beaveriot.blueprint.core.chart.node.data.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.SequenceNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

public interface FunctionNode extends DataNode, SequenceNode<DataNode> {

    String PREFIX = "fn::";

    @JsonIgnore
    String getFunctionName();

    List<DataNode> getParameters();

    void setParameters(List<DataNode> parameters);

    void setResult(DataNode data);

    DataNode getResult();

    @JsonIgnore
    List<BlueprintNode> getBlueprintNodeChildren();

    @JsonIgnore
    void setBlueprintNodeChildren(List<BlueprintNode> children);

    @Component
    abstract class Parser<T extends FunctionNode> implements BlueprintNode.Parser<T>, BlueprintRuntimeFunctionName {

        @Autowired
        private DataNode.Parser dataNodeParser;

        protected abstract T createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName);

        @Override
        public T parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode, BlueprintParseContext context) {
            if (!(propertyValue instanceof ObjectNode objectNode)) {
                return null;
            }

            var functionNode = createNode(parentNode, propertyName);
            var parameters = new ArrayList<DataNode>();
            var functionParameterNode = objectNode.get(this.getFunctionName());
            if (functionParameterNode instanceof ArrayNode arrayNode) {
                for (var i = arrayNode.size() - 1; i >= 0; i--) {
                    var itemName = "[" + i + "]";
                    var item = arrayNode.get(i);
                    context.pushTask(() -> parameters.add(dataNodeParser.parse(itemName, item, functionNode, context)));
                }
                functionNode.setParameters(parameters);
            } else {
                context.pushTask(() -> functionNode.setParameters(List.of(dataNodeParser.parse("[0]", functionParameterNode, functionNode, context))));
            }
            return functionNode;
        }

    }

}
