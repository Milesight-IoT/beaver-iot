package com.milesight.beaveriot.rule.flow.definition;

import org.apache.camel.CamelContext;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.springframework.core.annotation.Order;

/**
 * @author leon
 */
@Order(ComponentDefinitionLoader.ORDER_LEVEL_CAMEL)
public class CamelComponentDefinitionLoader implements ComponentDefinitionLoader {

    private CamelContext context;

    public CamelComponentDefinitionLoader(CamelContext context) {
        this.context = context;
    }

    @Override
    public String loadComponentDefinitionSchema(String name) {
        RuntimeCamelCatalog catalog = context.getCamelContextExtension().getContextPlugin(RuntimeCamelCatalog.class);
        return catalog.componentJSonSchema(name);
    }
}
