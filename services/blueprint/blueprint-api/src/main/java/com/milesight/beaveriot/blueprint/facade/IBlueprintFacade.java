package com.milesight.beaveriot.blueprint.facade;

import com.milesight.beaveriot.blueprint.support.TemplateLoader;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/9 16:02
 **/
public interface IBlueprintFacade {
    Long deployBlueprint(TemplateLoader templateLoader, Map<String, Object> variables);
}
