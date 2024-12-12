package com.milesight.beaveriot.rule.flow.definition;

import java.io.IOException;

/**
 * @author leon
 */
public interface ComponentDefinitionLoader {

    int ORDER_LEVEL_CUSTOMIZE_JSON = 0;
    int ORDER_LEVEL_ANNOTATION = 10;
    int ORDER_LEVEL_CAMEL = 20;

    String loadComponentDefinitionSchema(String name) throws IOException;

}
