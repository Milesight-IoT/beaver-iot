package com.milesight.beaveriot.blueprint.core.chart.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.support.TemplateLoader;
import org.springframework.lang.Nullable;

import java.util.Map;


public interface IBlueprintTemplateParser {
    @Nullable
    JsonNode getVariableJsonSchema(TemplateLoader templateLoader, Map<String, Object> context);

    TemplateNode parseBlueprint(TemplateLoader templateLoader, Map<String, Object> context);

    void loadConstantsIntoContext(TemplateLoader templateLoader, Map<String, Object> context);

    <T> T readTemplateAsType(TemplateLoader templateLoader, String relativePath, Map<String, Object> context, Class<T> clazz);

    JsonNode readTemplateAsJsonNode(TemplateLoader templateLoader, String relativePath, Map<String, Object> context);

    String readTemplateAsYaml(TemplateLoader templateLoader, String relativePath, Map<String, Object> context);
}
