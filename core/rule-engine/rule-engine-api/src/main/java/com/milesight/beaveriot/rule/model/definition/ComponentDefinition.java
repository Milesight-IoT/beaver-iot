package com.milesight.beaveriot.rule.model.definition;

import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
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

    protected final String PROPERTY_NODE_ID = "nodeId";

    public String generateUri(String id, RuleNodeConfig ruleNodeConfig, Map<String, Object> parameters) {

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
            if (!ObjectUtils.isEmpty(parameters) && parameters.containsKey(pathOptionDefinition.generateFullName())) {
                path = (String) parameters.get(pathOptionDefinition.generateFullName());
            }
        }

        return appendExtraUriParameters(generateUri(component.getScheme(), path), ruleNodeConfig);
    }

    private String appendExtraUriParameters(String uri, RuleNodeConfig ruleNodeConfig) {
        //append nodeId if need
        if (properties.containsKey(PROPERTY_NODE_ID) && properties.containsKey(PROPERTY_NODE_ID) && properties.get(PROPERTY_NODE_ID).isAutowired()) {
            uri = uri + "?" + PROPERTY_NODE_ID + "=" + ruleNodeConfig.getId();
        }
        return uri;
    }

    private String generateUri(String scheme, String path) {
        return scheme + ":" + path;
    }
}
