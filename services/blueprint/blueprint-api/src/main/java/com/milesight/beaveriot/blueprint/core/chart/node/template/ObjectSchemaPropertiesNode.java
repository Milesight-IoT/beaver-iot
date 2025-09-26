package com.milesight.beaveriot.blueprint.core.chart.node.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractMapNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@NoArgsConstructor
public class ObjectSchemaPropertiesNode extends AbstractMapNode<DataNode> {

    public ObjectSchemaPropertiesNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @Override
    public BlueprintNodeStatus getBlueprintNodeStatus() {
        return BlueprintNodeStatus.FINISHED;
    }

    @Component
    public static class Parser implements BlueprintNode.Parser<ObjectSchemaPropertiesNode> {

        @Autowired
        private DataNode.Parser dataNodeParser;

        @Override
        public ObjectSchemaPropertiesNode parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode,
                                                BlueprintParseContext context) {
            var objectSchemaPropertiesNode = new ObjectSchemaPropertiesNode(parentNode, propertyName);
            if (propertyValue instanceof ObjectNode objectNode) {
                BlueprintUtils.forEachInReverseOrder(objectNode.fields(), entry ->
                        context.pushTask(() -> objectSchemaPropertiesNode.addChildNode(dataNodeParser.parse(entry.getKey(), entry.getValue(), objectSchemaPropertiesNode, context))));
            }
            return objectSchemaPropertiesNode;
        }
    }

}
