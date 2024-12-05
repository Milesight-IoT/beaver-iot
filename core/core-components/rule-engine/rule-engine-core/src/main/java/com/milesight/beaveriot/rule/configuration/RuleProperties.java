package com.milesight.beaveriot.rule.configuration;

import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.definition.BaseDefinition;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
@Data
@ConfigurationProperties(prefix = "camel.rule")
public class RuleProperties {

    /**
     * The components defined in the rule engine. The key is the node type, the value is the list of components
     */
    private Map<RuleNodeType, List<BaseDefinition>> components;

    /**
     * The language properties, such as code and expression languages
     */
    private RuleLanguage languages;

    /**
     * Whether to enable tracing. Default is true.
     */
    private boolean enabledTracing = true;

    /**
     * The trace output mode. default is ALL
     */
    private TraceOutputMode traceOutputMode = TraceOutputMode.EVENT;

    /**
     * The path to the component schema directory. Default is /camel-schema.
     * The schema file is named as {componentName}.json, used to override the camel component's options.
     */
    private String componentSchemaPath = "/camel-schema";


    public enum TraceOutputMode {
        LOGGING, EVENT, ALL
    }
}
