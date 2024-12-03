package com.milesight.beaveriot.sample;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.sample.enums.DeviceStatus;
import org.apache.camel.CamelContext;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */
@Component
public class AnnotationSampleIntegrationBootstrap implements IntegrationBootstrap {

    @Override
    public void onPrepared(Integration integrationConfig) {

        Device device = new DeviceBuilder(integrationConfig.getId())
                .name("deviceDemo1")
                .identifier("deviceDemo1")
                .entity(()->{
                    return new EntityBuilder()
                            .identifier("propParent")
                            .property("propParent", AccessMod.W)
                            .attributes(new AttributeBuilder().maxLength(100).enums(DeviceStatus.class).unit("ms").build())
                            .valueType(EntityValueType.STRING)
                            .children()
                                .valueType(EntityValueType.STRING).property("propChildren1", AccessMod.W).end()
                            .children()
                                .valueType(EntityValueType.STRING).property("propChildren2", AccessMod.W).end()
                            .build();
                })
                .build();
        integrationConfig.addInitialDevice(device);

    }

    @Override
    public void onStarted(Integration integrationConfig) {

    }

    @Override
    public void onDestroy(Integration integrationConfig) {
    }

    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        IntegrationBootstrap.super.customizeRoute(context);
    }
}
