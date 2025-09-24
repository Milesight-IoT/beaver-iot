package com.milesight.beaveriot.blueprint.core.chart.node.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractObjectNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.KeyValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.constant.BlueprintConstants;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@FieldNameConstants
@NoArgsConstructor
public class TemplateNode extends AbstractObjectNode {

    private ResourcesNode resources;

    private IncludesNode include;

    private ObjectSchemaPropertiesNode parameters;

    private DataNode parameterValues;

    public TemplateNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @JsonIgnore
    public void setParameterValues(DataNode parameterValues) {
        this.parameterValues = parameterValues;
    }

    @JsonIgnore
    public DataNode getParameterValues() {
        return parameterValues;
    }

    @Component
    public static class Parser implements BlueprintNode.Parser<TemplateNode> {

        @Autowired
        private ResourcesNode.Parser resourcesNodeParser;

        @Autowired
        private IncludesNode.Parser includesNodeParser;

        @Autowired
        private ObjectSchemaPropertiesNode.Parser parametersNodeParser;

        @Override
        public TemplateNode parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode,
                                  BlueprintParseContext context) {
            var templateNode = new TemplateNode(parentNode, propertyName);

            context.pushTask(() -> initTemplateParameterValues(templateNode, context));

            if (propertyValue.get(Fields.resources) instanceof ObjectNode resourcesJsonNode) {
                context.pushTask(() -> templateNode.setResources(
                        resourcesNodeParser.parse(Fields.resources, resourcesJsonNode, templateNode, context)));
            }
            if (propertyValue.get(Fields.include) instanceof ObjectNode includesJsonNode) {
                context.pushTask(() -> templateNode.setInclude(
                        includesNodeParser.parse(Fields.include, includesJsonNode, templateNode, context)));
            }
            if (propertyValue.get(Fields.parameters) instanceof ObjectNode parametersJsonNode) {
                context.pushTask(() -> templateNode.setParameters(
                        parametersNodeParser.parse(Fields.parameters, parametersJsonNode, templateNode, context)));
            }
            return templateNode;
        }

        @SuppressWarnings("unchecked")
        private static void initTemplateParameterValues(TemplateNode templateNode, BlueprintParseContext context) {
            if (templateNode.getParameterValues() != null) {
                return;
            }

            var parameterValues = new ParameterValues(templateNode, StringUtils.toSnakeCase(TemplateNode.Fields.parameterValues));

            if (templateNode.getBlueprintNodeParent() instanceof IncludeNode includeNode) {
                // link to parameter values defined in parent template
                includeNode.getParameters().getBlueprintNodeChildren().forEach(parameterValues::addChildNode);
            }

            if (templateNode == context.getRoot()) {
                var parameters = (Map<String, Object>) context.getTemplateContext().getOrDefault(BlueprintConstants.PARAMETERS_KEY, Collections.emptyMap());
                parameters.forEach((key, value) -> parameterValues.addChildNode(BlueprintUtils.convertToDataNode(key, parameterValues, value)));
            }

            var nameToParameter = parameterValues.getBlueprintNodeChildren().stream()
                    .collect(Collectors.toMap(BlueprintNode::getBlueprintNodeName, Function.identity(), (a, b) -> a));
            var parameterSchemas = templateNode.getParameters();
            var defaultValues = getDefaultValueDataNodes(parameterSchemas, parameterValues);
            for (var defaultValue : defaultValues.values()) {
                var param = nameToParameter.get(defaultValue.getBlueprintNodeName());
                if (param == null) {
                    defaultValue.setBlueprintNodeParent(parameterSchemas);
                    if (parameterValues.getChild(defaultValue.getBlueprintNodeName()) == null) {
                        parameterValues.addChildNode(defaultValue);
                    }
                }
            }

            templateNode.setParameterValues(parameterValues);
        }

        private static Map<String, DataNode> getDefaultValueDataNodes(ObjectSchemaPropertiesNode schema, BlueprintNode parent) {
            return schema.getBlueprintNodeChildren().stream()
                    .map(child -> {
                        if (child instanceof FunctionNode f) {
                            child = f.getResult();
                        }
                        if (child instanceof KeyValueNode<?> kv
                                && (kv.getChild("default") instanceof DataNode defaultValue)) {
                            defaultValue = JsonUtils.copy(defaultValue);
                            defaultValue.setBlueprintNodeName(child.getBlueprintNodeName());
                            defaultValue.setBlueprintNodeParent(parent);
                            return defaultValue;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toConcurrentMap(BlueprintNode::getBlueprintNodeName, Function.identity(), (a, b) -> a));
        }

    }
}
