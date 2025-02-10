package com.milesight.beaveriot.sample.entity;


import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.sample.enums.DeviceStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class DemoIntegrationEntities extends ExchangePayload {

    // Service Samples
    @Entity(type = EntityType.SERVICE, attributes = @Attribute(unit = "m", max = 1, min = 0, enumClass = DeviceStatus.class, optional = true))
    private String entitySync;
    @Entity(type = EntityType.SERVICE, attributes = @Attribute(optional = true))
    private String deviceSync;
    @Entity(type = EntityType.SERVICE)
    private Boolean boolService;

    @Entity(type = EntityType.SERVICE)
    private DemoGroupSettingEntities connect;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DemoGroupSettingEntities extends ExchangePayload {
        @Entity
        private String accessKey;
        @Entity
        private String secretKey;
        @Entity
        private Long length;
    }

    // Property Samples
    @Entity(type = EntityType.PROPERTY, name="Property Read Only", accessMod = AccessMod.R)
    private String propertyReadOnly;

    @Entity(type = EntityType.PROPERTY, name="Property Read Write", accessMod = AccessMod.RW)
    private String propertyReadWrite;

    @Entity(type = EntityType.PROPERTY, name="Property Read Only Enum", attributes = @Attribute(enumClass = DeviceStatus.class), accessMod = AccessMod.R)
    private String propertyReadOnlyEnum;

    @Entity(type = EntityType.PROPERTY, name="Property Read Write Enum", attributes = @Attribute(enumClass = DeviceStatus.class), accessMod = AccessMod.RW)
    private String propertyReadWriteEnum;

    @Entity(type = EntityType.PROPERTY, name="Boolean Read Write", accessMod = AccessMod.RW)
    private Boolean propertyBooleanReadWrite;

    @Entity(type = EntityType.PROPERTY, name="Boolean Read Only", accessMod = AccessMod.R)
    private Boolean propertyBooleanReadOnly;

    @Entity(type = EntityType.PROPERTY, name="Long Read Write", accessMod = AccessMod.RW)
    private Long propertyLongReadWrite;

    @Entity(type = EntityType.PROPERTY, name="Double Read Write", accessMod = AccessMod.RW)
    private Double propertyDoubleReadWrite;

    @Entity(type = EntityType.PROPERTY, name="Property Group", accessMod = AccessMod.RW)
    private SamplePropertyEntities propertyGroup;

    @Entity(type = EntityType.PROPERTY, name="Property Group Read Only", accessMod = AccessMod.R)
    private SamplePropertyEntities propertyGroupReadonly;

    // Property Samples
    @Entity(type = EntityType.EVENT, name="Event")
    private String eventStringSample;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class SamplePropertyEntities extends ExchangePayload {
        @Entity
        private String prop1;
        @Entity
        private String prop2;
    }
}
