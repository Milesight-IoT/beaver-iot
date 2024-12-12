package com.milesight.beaveriot.rule.flow.definition;

import com.milesight.beaveriot.rule.configuration.RuleProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author leon
 */
@Order(ComponentDefinitionLoader.ORDER_LEVEL_CUSTOMIZE_JSON)
public class CustomizeJsonComponentDefinitionLoader implements ComponentDefinitionLoader {

    private static final String SLASH = "/";

    private RuleProperties ruleProperties;

    public CustomizeJsonComponentDefinitionLoader(RuleProperties ruleProperties) {
        this.ruleProperties = ruleProperties;
    }

    @Override
    public String loadComponentDefinitionSchema(String name) throws IOException {

        String path = ruleProperties.getComponentSchemaPath().endsWith(SLASH) ?
                ruleProperties.getComponentSchemaPath() + name + ".json" :
                ruleProperties.getComponentSchemaPath() + SLASH + name + ".json";

        ClassPathResource classPathResource = new ClassPathResource(path);
        return classPathResource.exists() ? classPathResource.getContentAsString(Charset.defaultCharset()) : null;
    }
}
