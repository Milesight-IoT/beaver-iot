package com.milesight.beaveriot.context.integration.model;

/**
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
