package com.milesight.beaveriot.blueprint.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.blueprint.support.ResourceLoader;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/9 16:02
 **/
public interface IBlueprintFacade {

    Long deployBlueprint(ResourceLoader resourceLoader, Map<String, Object> variables);


    void removeBlueprint(Long blueprintId);


    JsonNode getVariableJsonSchema(ResourceLoader resourceLoader);

}
