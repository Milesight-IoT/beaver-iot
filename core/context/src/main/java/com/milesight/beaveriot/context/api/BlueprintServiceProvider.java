package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.support.TemplateLoader;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/9 16:02
 **/
public interface BlueprintServiceProvider {
    Long deployBlueprint(TemplateLoader templateLoader, Map<String, Object> variables);
}
