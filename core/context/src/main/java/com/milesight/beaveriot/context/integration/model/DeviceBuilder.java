package com.milesight.beaveriot.context.integration.model;

/**
 * DeviceBuilder is a builder class for Device, eg:
 * Entity entityConfig = new EntityBuilder()
 * .property("humidity", AccessMod.RW)
 * .identifier("humidity")
 * .children()
 * .property("value", AccessMod.RW)
 * .end()
 * .children()
 * .property("unit", AccessMod.RW)
 * .end()
 * .children()
 * .property("timestamp", AccessMod.RW)
 * .end()
 * .build();
 * <p>
 * Device device = new DeviceBuilder("myIntegrationId"，"myIntegration")
 * .name("myDevice")
 * .identifier("mySN")
 * .entity(entityConfig)
 * .build();
 *
 * @author leon
 */
public class DeviceBuilder extends BaseDeviceBuilder<DeviceBuilder> {

    public DeviceBuilder(String integrationId) {
        super(integrationId);
    }

    public static class IntegrationDeviceBuilder extends BaseDeviceBuilder<IntegrationDeviceBuilder> {
        protected IntegrationBuilder integrationBuilder;

        public IntegrationDeviceBuilder(IntegrationBuilder integrationBuilder) {
            super(integrationBuilder.integration.getId());
            this.integrationBuilder = integrationBuilder;
        }

        public IntegrationBuilder end() {
            integrationBuilder.integration.addInitialDevice(build());
            return integrationBuilder;
        }
    }

}
