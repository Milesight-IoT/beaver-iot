package com.milesight.beaveriot.blueprint.core.chart.node.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractMapNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@NoArgsConstructor
public class IncludesNode extends AbstractMapNode<IncludeNode> {

    public IncludesNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @Component
    public static class Parser implements BlueprintNode.Parser<IncludesNode> {

        @Autowired
        private IncludeNode.Parser includeNodeParser;

        @Override
        public IncludesNode parse(String propertyName, JsonNode propertyValue,
                                  BlueprintNode parentNode, BlueprintParseContext context) {
            var includesNode = new IncludesNode(parentNode, propertyName);
            includesNode.setBlueprintNodeName(propertyName);
            if (propertyValue instanceof ObjectNode object) {
                BlueprintUtils.forEachInReverseOrder(object.fields(), entry -> context.pushTask(() -> includesNode.addChildNode(
                        includeNodeParser.parse(entry.getKey(), entry.getValue(), includesNode, context))));
            }
            return includesNode;
        }
    }

}
