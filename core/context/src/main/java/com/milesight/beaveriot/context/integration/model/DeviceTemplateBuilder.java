package com.milesight.beaveriot.context.integration.model;

/**
 *
 * @author leon
 */
public class DeviceTemplateBuilder extends BaseDeviceTemplateBuilder<DeviceTemplateBuilder> {

    public DeviceTemplateBuilder(String integrationId) {
        super(integrationId);
    }

    public static class IntegrationDeviceTemplateBuilder extends BaseDeviceBuilder<IntegrationDeviceTemplateBuilder> {
        protected IntegrationBuilder integrationBuilder;

        public IntegrationDeviceTemplateBuilder(IntegrationBuilder integrationBuilder) {
            super(integrationBuilder.integration.getId());
            this.integrationBuilder = integrationBuilder;
        }

        public IntegrationBuilder end() {
            integrationBuilder.integration.addInitialDevice(build());
            return integrationBuilder;
        }
    }

}
