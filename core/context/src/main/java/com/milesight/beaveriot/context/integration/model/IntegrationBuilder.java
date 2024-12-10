package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 *
 * @author leon
 */
public class IntegrationBuilder {

    protected Integration integration;

    public DeviceBuilder.IntegrationDeviceBuilder initialDevice(String name, String identifier) {
        DeviceBuilder.IntegrationDeviceBuilder deviceBuilder = new DeviceBuilder.IntegrationDeviceBuilder(this);
        return deviceBuilder.name(name).identifier(identifier);
    }

    public DeviceBuilder.IntegrationDeviceBuilder initialDevice() {
        return new DeviceBuilder.IntegrationDeviceBuilder(this);
    }

    public DeviceBuilder.IntegrationDeviceBuilder initialDevice(String name, String identifier, Map<String, Object> additional) {
        DeviceBuilder.IntegrationDeviceBuilder deviceBuilder = new DeviceBuilder.IntegrationDeviceBuilder(this);
        return deviceBuilder.name(name).identifier(identifier).additional(additional);
    }

    public IntegrationBuilder initialDevice(Device device) {
        Assert.notNull(integration, "integration can't be null, please set integration first");
        integration.addInitialDevice(device);
        return this;
    }

    public IntegrationBuilder initialDevices(List<Device> devices) {
        Assert.notNull(integration, "integration can't be null, please set integration first");
        integration.addInitialDevices(devices);
        return this;
    }

    public IntegrationBuilder initialDevices(Supplier<List<Device>> supplier) {
        return initialDevices(supplier.get());
    }


    public IntegrationBuilder initialEntity(Entity entity) {
        Assert.notNull(integration, "integration can't be null, please set integration first");
        integration.addInitialEntity(entity);
        return this;
    }

    public IntegrationBuilder initialEntities(List<Entity> entities) {
        Assert.notNull(integration, "integration can't be null, please set integration first");
        integration.addInitialEntities(entities);
        return this;
    }

    public IntegrationBuilder initialEntities(Supplier<List<Entity>> supplier) {
        return initialEntities(supplier.get());
    }

    public IntegrationConfigBuilder integration() {
        this.integration = new Integration();
        return new IntegrationConfigBuilder(this);
    }

    public IntegrationBuilder integration(String id, String name) {
        Assert.isNull(integration, "Integration can't repeat Settings");
        this.integration = Integration.of(id, name);
        return this;
    }

    public IntegrationBuilder integration(Integration integration) {
        Assert.isNull(integration, "Integration can't repeat Settings");
        this.integration = integration;
        return this;
    }

    public Integration build() {
        integration.initializeProperties();
        return integration;
    }

    public static class IntegrationConfigBuilder {

        private IntegrationBuilder integrationBuilder;

        public IntegrationConfigBuilder(IntegrationBuilder integrationBuilder) {
            this.integrationBuilder = integrationBuilder;
        }

        public IntegrationConfigBuilder id(String id) {
            this.integrationBuilder.integration.setId(id);
            return this;
        }

        public IntegrationConfigBuilder enabled(boolean enabled) {
            this.integrationBuilder.integration.setEnabled(enabled);
            return this;
        }

        public IntegrationConfigBuilder name(String name) {
            this.integrationBuilder.integration.setName(name);
            return this;
        }

        public IntegrationConfigBuilder description(String description) {
            this.integrationBuilder.integration.setDescription(description);
            return this;
        }

        public IntegrationConfigBuilder iconUrl(String iconUrl) {
            this.integrationBuilder.integration.setIconUrl(iconUrl);
            return this;
        }

        public IntegrationConfigBuilder integrationClass(Class<? extends IntegrationBootstrap> integrationClass) {
            this.integrationBuilder.integration.setIntegrationClass(integrationClass);
            return this;
        }

        public IntegrationConfigBuilder entityIdentifierAddDevice(String addDeviceEntityIdentifier) {
            this.integrationBuilder.integration.setEntityIdentifierAddDevice(addDeviceEntityIdentifier);
            return this;
        }

        public IntegrationConfigBuilder entityIdentifierDeleteDevice(String deleteDeviceEntityIdentifier) {
            this.integrationBuilder.integration.setEntityIdentifierDeleteDevice(deleteDeviceEntityIdentifier);
            return this;
        }

        public IntegrationBuilder end() {
            return integrationBuilder;
        }
    }
}
