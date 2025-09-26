package com.milesight.beaveriot.blueprint.core.chart.deploy;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;


@Data
@RequiredArgsConstructor
public class BlueprintDeployContext {

    private final TemplateNode root;

    private final JsonNode variables;

    private final Map<String, Object> templateContext;

}
