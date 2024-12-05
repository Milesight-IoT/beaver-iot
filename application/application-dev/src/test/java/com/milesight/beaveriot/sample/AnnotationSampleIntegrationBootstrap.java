package com.milesight.beaveriot.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.context.integration.IntegrationContext;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrapManager;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedTemplateEntityWrapper;
import com.milesight.beaveriot.context.integration.wrapper.EntityWrapper;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.sample.entity.DemoDeviceEntities;
import com.milesight.beaveriot.sample.entity.DemoIntegrationEntities;
import com.milesight.beaveriot.sample.enums.DeviceStatus;
import org.apache.camel.CamelContext;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author leon
 */
@Component
public class AnnotationSampleIntegrationBootstrap implements IntegrationBootstrap {

    @Override
    public void onPrepared(Integration integrationConfig) {
        Device build = new DeviceBuilder("demo-anno-integration")
                .identifier("deviceId")
                .name("deviceName")
                .entities(() -> {
                    List<Entity> build1 = new AnnotatedTemplateEntityBuilder("demo-anno-integration", "deviceId")
                            .build(DemoDeviceEntities.class);
                    return build1;
                }).build();

        integrationConfig.addInitialDevice(build);

    }

    @Override
    public void onStarted(Integration integrationConfig) {

        integrationConfig.getEnvironment();

        AnnotatedTemplateEntityWrapper<DemoDeviceEntities> wrapper = new AnnotatedTemplateEntityWrapper<>("deviceId");
        wrapper.saveValue(DemoDeviceEntities::getTemperature, 20.0)
                .publish();
        wrapper.saveValues(Map.of(DemoDeviceEntities::getTemperature, 20.0,DemoDeviceEntities::getHumidity, 20.0))
                .publish(ExchangeEvent.EventType.DOWN, eventResponse -> System.out.println(eventResponse));
        Optional<Double> value = wrapper.getValue(DemoDeviceEntities::getTemperature, Double.class);
        Map<String, JsonNode> values = wrapper.getValues(DemoDeviceEntities::getTemperature, DemoDeviceEntities::getHumidity);

        AnnotatedTemplateEntityWrapper<DemoDeviceEntities.DemoGroupDeviceEntities> wrapper2 = new AnnotatedTemplateEntityWrapper<>("deviceId");
        wrapper2.saveValue(DemoDeviceEntities.DemoGroupDeviceEntities::getChildStatus, "20.0").publish();
        Optional<String> value1 = wrapper2.getValue(DemoDeviceEntities.DemoGroupDeviceEntities::getChildStatus, String.class);

        AnnotatedEntityWrapper<DemoIntegrationEntities> wrapper3 = new AnnotatedEntityWrapper<>();
        wrapper3.saveValues(Map.of(DemoIntegrationEntities::getEntitySync, "20.0", DemoIntegrationEntities::getDeviceSync, "deviceName"))
                .publish();
        Map<String, JsonNode> values1 = wrapper3.getValues(DemoIntegrationEntities::getEntitySync, DemoIntegrationEntities::getDeviceSync);


        Entity entity = new EntityBuilder("demo-anno-integration").identifier("property_group").service("property_group").valueType(EntityValueType.STRING)
                .children().identifier("prop1").service("prop1").valueType(EntityValueType.STRING).end()
                .children().identifier("prop3").service("prop2").valueType(EntityValueType.STRING).end()
                .build();
        EntityWrapper entityWrapper = new EntityWrapper(entity);
//        entityWrapper.saveValue("22").publish();
        entityWrapper.saveValues(Map.of("prop1","123", "prop2","456")).publish();
        Optional<String> value2 = entityWrapper.getValue(String.class);


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
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
    }

    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        IntegrationBootstrap.super.customizeRoute(context);
    }
}
