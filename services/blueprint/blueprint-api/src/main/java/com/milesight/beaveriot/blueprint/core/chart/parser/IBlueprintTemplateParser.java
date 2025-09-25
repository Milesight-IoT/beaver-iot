package com.milesight.beaveriot.blueprint.core.chart.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.support.ResourceLoader;
import org.springframework.lang.Nullable;

import java.util.Map;


public interface IBlueprintTemplateParser {
    @Nullable
    JsonNode getVariableJsonSchema(ResourceLoader resourceLoader, Map<String, Object> context);

    TemplateNode parseBlueprint(ResourceLoader resourceLoader, Map<String, Object> context);

    void loadConstantsIntoContext(ResourceLoader resourceLoader, Map<String, Object> context);

    <T> T readTemplateAsType(ResourceLoader resourceLoader, String relativePath, Map<String, Object> context, Class<T> clazz);

    JsonNode readTemplateAsJsonNode(ResourceLoader resourceLoader, String relativePath, Map<String, Object> context);

    String readTemplateAsYaml(ResourceLoader resourceLoader, String relativePath, Map<String, Object> context);
}
