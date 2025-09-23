package com.milesight.beaveriot.blueprint.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.core.chart.node.BlueprintParseContext;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.core.chart.parser.IBlueprintTemplateParser;
import com.milesight.beaveriot.blueprint.core.constant.BlueprintConstants;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.model.ConstantsTemplate;
import com.milesight.beaveriot.blueprint.core.model.ParametersObjectSchema;
import com.milesight.beaveriot.blueprint.core.model.VariablesTemplate;
import com.milesight.beaveriot.blueprint.support.TemplateLoader;
import io.pebbletemplates.pebble.PebbleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Map;

@Slf4j
@Component
public class BlueprintTemplateParser implements IBlueprintTemplateParser {

    @Autowired
    private PebbleEngine pebbleEngine;

    @Autowired
    private Yaml blueprintSnakeYaml;

    @Autowired
    private TemplateNode.Parser templateNodeParser;

    @Override
    @Nullable
    public JsonNode getVariableJsonSchema(TemplateLoader templateLoader, Map<String, Object> context) {
        var variables = readTemplateAsType(templateLoader, BlueprintConstants.VARIABLES_TEMPLATE_FILE_NAME, context, VariablesTemplate.class);
        if (variables == null) {
            log.info("variables.peb not found.");
            return null;
        }
        return JsonUtils.toJsonNode(new ParametersObjectSchema(variables.getProperties()));
    }

    @Override
    public TemplateNode parseBlueprint(TemplateLoader templateLoader, Map<String, Object> context) {
        var indexTemplateJsonNode = readTemplateAsJsonNode(templateLoader, BlueprintConstants.INDEX_TEMPLATE_FILE_NAME, context);
        if (indexTemplateJsonNode == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_TEMPLATE_PARSING_FAILED, "index.peb not found.");
        }

        var tasks = new ArrayDeque<BlueprintNode.ProcessingTask>();
        var blueprintChartContext = new BlueprintParseContext(tasks, templateLoader, indexTemplateJsonNode, context);
        var root = templateNodeParser.parse(null, indexTemplateJsonNode, null, blueprintChartContext);
        blueprintChartContext.setRoot(root);

        while (!tasks.isEmpty()) {
            tasks.pop().process();
        }

        return root;
    }

    @Override
    public void loadConstantsIntoContext(TemplateLoader templateLoader, Map<String, Object> context) {
        var constantsTemplate = readTemplateAsType(templateLoader, BlueprintConstants.CONSTANTS_TEMPLATE_FILE_NAME, context, ConstantsTemplate.class);
        if (constantsTemplate != null && !CollectionUtils.isEmpty(constantsTemplate.getValues())) {
            context.put(BlueprintConstants.CONSTANTS_KEY, constantsTemplate.getValues());
        }
    }

    @Override
    public <T> T readTemplateAsType(TemplateLoader templateLoader, String relativePath, Map<String, Object> context, Class<T> clazz) {
        return JsonUtils.cast(readTemplateAsJsonNode(templateLoader, relativePath, context), clazz);
    }

    @Override
    public JsonNode readTemplateAsJsonNode(TemplateLoader templateLoader, String relativePath, Map<String, Object> context) {
        var yaml = readTemplateAsYaml(templateLoader, relativePath, context);
        if (yaml == null) {
            return null;
        }
        return JsonUtils.toJsonNode(blueprintSnakeYaml.load(yaml));
    }


    @Override
    public String readTemplateAsYaml(TemplateLoader templateLoader, String relativePath, Map<String, Object> context) {
        try (var templateInputStream = templateLoader.loadTemplate(relativePath)) {
            if (templateInputStream == null) {
                return null;
            }
            var templateString = StringUtils.copyToString(templateInputStream);
            log.debug("read template {}: {}", relativePath, templateString);
            var template = pebbleEngine.getTemplate(templateString);
            var stringWriter = new StringWriter();
            template.evaluate(stringWriter, context);
            var result = stringWriter.toString();
            log.debug("pebble evaluated: {}", result);
            return result;
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.SERVER_ERROR, "Failed to read template " + relativePath, e);
        }
    }

}
