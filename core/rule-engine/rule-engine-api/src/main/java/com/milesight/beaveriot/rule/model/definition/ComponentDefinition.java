package com.milesight.beaveriot.rule.model.definition;

import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author leon
 */
@Data
public class ComponentDefinition {

    private ComponentBaseDefinition component = new ComponentBaseDefinition();

    protected final Map<String, ComponentOptionDefinition> exchangeProperties = new LinkedHashMap<>();
    protected final Map<String, ComponentOptionDefinition> headers = new LinkedHashMap<>();
    protected final Map<String, ComponentOptionDefinition> properties = new LinkedHashMap<>();
    protected final Map<String, ComponentOutputDefinition> outputProperties = new LinkedHashMap<>();

    public String generateUri(String id, Map<String, Object> parameters) {

        //if component is bean, then use bean name as uri
        if ("bean".equals(component.getScheme())) {
            return generateUri(component.getScheme(), component.getName());
        }

        //if component contains path property, and parameters has path value, then use path as uri
        ComponentOptionDefinition pathOptionDefinition = properties.values().stream()
                .filter(definition -> definition.getKind().equals("path"))
                .findFirst()
                .orElse(null);
        String path = id;
        if (pathOptionDefinition != null) {
            if (ObjectUtils.isEmpty(parameters) || !parameters.containsKey(pathOptionDefinition.generateFullName())) {
                //else use id as uri
                if (pathOptionDefinition.isAutowired()) {
                    parameters.put(pathOptionDefinition.generateFullName(), id);
                }
            } else {
                path = (String) parameters.get(pathOptionDefinition.generateFullName());
            }
        }

        return generateUri(component.getScheme(), path);
    }

    private String generateUri(String scheme, String path) {
        return scheme + ":" + path;
    }
}
