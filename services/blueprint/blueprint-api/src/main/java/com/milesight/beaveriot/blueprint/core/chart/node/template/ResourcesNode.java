package com.milesight.beaveriot.blueprint.core.chart.node.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractMapNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.AbstractResourceNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor
public class ResourcesNode extends AbstractMapNode<AbstractResourceNode> {

    public ResourcesNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @Component
    public static class Parser implements BlueprintNode.Parser<ResourcesNode> {

        @Autowired
        private List<AbstractResourceNode.Parser<? extends AbstractResourceNode>> resourceNodeParsers;

        private Map<String, AbstractResourceNode.Parser<? extends AbstractResourceNode>> typeToResourceNodeParser;

        @Override
        public ResourcesNode parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode,
                                   BlueprintParseContext context) {

            if (typeToResourceNodeParser == null) {
                typeToResourceNodeParser = resourceNodeParsers.stream()
                        .collect(Collectors.toConcurrentMap(AbstractResourceNode.Parser::getResourceType, Function.identity(),
                                (a, b) -> a));
            }

            var resourcesNode = new ResourcesNode(parentNode, propertyName);
            if (propertyValue instanceof ObjectNode object) {
                BlueprintUtils.forEachInReverseOrder(object.fields(), entry -> Optional.ofNullable(entry.getValue().get("type"))
                        .filter(TextNode.class::isInstance)
                        .map(JsonNode::asText)
                        .map(typeToResourceNodeParser::get)
                        .ifPresent(parser -> context.pushTask(() -> resourcesNode.addChildNode(
                                parser.parse(entry.getKey(), entry.getValue(), resourcesNode, context)))));
            }
            return resourcesNode;
        }
    }

}
