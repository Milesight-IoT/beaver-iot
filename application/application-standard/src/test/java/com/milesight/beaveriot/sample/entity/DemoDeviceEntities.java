package com.milesight.beaveriot.sample.entity;


import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.DeviceTemplateEntities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceBuilder;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Define entities by annotation
 *
 * @author leon
 */
@Data
@DeviceTemplateEntities(name="demoDevice")
public class DemoDeviceEntities extends ExchangePayload {
    @Entity(name = "temperature", identifier = "temperature", accessMod = AccessMod.RW, type = EntityType.PROPERTY, description = "abc")
    public Double temperature;

    @Entity
    public String humidity;

    @Entity(attributes = {@Attribute(unit = "ms", min = 0, max = 1000)})
    public Integer status;

    @Entity(type= EntityType.SERVICE)
    public String changeStatus;

    @Entity(type= EntityType.SERVICE)
    public DemoGroupDeviceEntities groupDeviceEntities;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DemoGroupDeviceEntities extends ExchangePayload {
        @Entity
        public String childStatus;
    }

    public static void main(String[] args) {
        com.milesight.beaveriot.context.integration.model.Entity entityConfig = new EntityBuilder()
                .property("prop_parent", AccessMod.W)
                .valueType(EntityValueType.STRING)
                .children()
                .valueType(EntityValueType.STRING).property("prop_children1", AccessMod.W).end()
                .children()
                .valueType(EntityValueType.STRING).property("prop_children2", AccessMod.W).end()
                .build();
        Device device = new DeviceBuilder("myid")
                .name("complexDevice1")
                .identifier("complexDevice1")
                .entity(entityConfig)
                .build();
        System.out.println(device);
    }
}
