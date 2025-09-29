package com.milesight.beaveriot.blueprint.core.chart.node.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractObjectNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.parser.IBlueprintTemplateParser;
import com.milesight.beaveriot.blueprint.core.constant.BlueprintConstants;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@EqualsAndHashCode(callSuper = true)
@Data
@FieldNameConstants
@NoArgsConstructor
public class IncludeNode extends AbstractObjectNode {

    private TemplateNode template;

    private DataNode parameters;

    public IncludeNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @Component
    public static class Parser implements BlueprintNode.Parser<IncludeNode> {

        @Lazy
        @Autowired
        private TemplateNode.Parser templateNodeParser;

        @Autowired
        private DataNode.Parser dataNodeParser;

        @Lazy
        @Autowired
        private IBlueprintTemplateParser blueprintTemplateParser;

        @Override
        public IncludeNode parse(String propertyName, JsonNode propertyValue, BlueprintNode parentNode, BlueprintParseContext context) {
            var includeNode = new IncludeNode(parentNode, propertyName);

            if (propertyValue.get(Fields.template) instanceof TextNode templatePath) {
                var templateJsonNode = readTemplateAsJsonNode(propertyValue, context, templatePath);
                context.goIntoNestedTemplate();
                context.pushTask(() -> includeNode.setTemplate(templateNodeParser.parse(Fields.template, templateJsonNode, includeNode, context)));
                context.leaveFromNestedTemplate();
            } else {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_TEMPLATE_PARSING_FAILED, "Property 'template' should be a valid relative path.");
            }

            if (propertyValue.get(Fields.parameters) instanceof ObjectNode parameters) {
                context.pushTask(() -> includeNode.setParameters(dataNodeParser.parse(Fields.parameters, parameters, includeNode, context)));
            }
            return includeNode;
        }

        private JsonNode readTemplateAsJsonNode(JsonNode propertyValue, BlueprintParseContext context, TextNode templatePath) {
            var parameterValues = new HashMap<String, Object>();
            var templateContext = new HashMap<>(context.getTemplateContext());
            if (propertyValue.get(Fields.parameters) instanceof ObjectNode parameterObject) {
                parameterValues.putAll(JsonUtils.toMap(parameterObject));
            }
            templateContext.put(BlueprintConstants.PARAMETERS_KEY, parameterValues);

            var templatePathStr = templatePath.asText();
            var preRenderedTemplateJsonNode = blueprintTemplateParser.readTemplateAsJsonNode(
                    context.getResourceLoader(), templatePathStr, templateContext);
            if (preRenderedTemplateJsonNode == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_TEMPLATE_PARSING_FAILED, "Template not found: " + templatePathStr);
            }

            if (preRenderedTemplateJsonNode.get(TemplateNode.Fields.parameters) instanceof ObjectNode parameterSchema) {
                var defaultValues = new HashMap<String, Object>();
                BlueprintUtils.loadObjectSchemaDefaultValues(parameterSchema, defaultValues);
                defaultValues.forEach((key, value) -> parameterValues.computeIfAbsent(key, k -> value));
            }

            return blueprintTemplateParser.readTemplateAsJsonNode(context.getResourceLoader(), templatePathStr, templateContext);
        }

    }

}
