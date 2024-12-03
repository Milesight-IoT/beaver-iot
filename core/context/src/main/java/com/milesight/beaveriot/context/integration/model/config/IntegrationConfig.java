package com.milesight.beaveriot.context.integration.model.config;

import com.milesight.beaveriot.context.integration.model.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
@Data
public class IntegrationConfig {

    private String name;

    private String description;

    private String iconUrl;

    private boolean enabled = true;

    private String flowExchangeDownHandler;

    private String entityIdentifierAddDevice;

    private String entityIdentifierDeleteDevice;

    private List<DeviceConfig> initialDevices = new ArrayList<>();

    private List<EntityConfig> initialEntities = new ArrayList<>();

    public Integration toIntegration(String integrationId) {

        List<Device> devices = initialDevices.stream()
                .map(deviceConfig -> new DeviceBuilder(integrationId)
                        .name(deviceConfig.getName())
                        .identifier(deviceConfig.getIdentifier())
                        .entities(()-> deviceConfig.getEntities().stream().map(entityConfig -> entityConfig.toEntity()).toList())
                        .build()
                )
                .toList();

        return new IntegrationBuilder()
                .integration()
                .id(integrationId)
                .name(name)
                .description(description)
                .iconUrl(iconUrl)
                .enabled(enabled)
                .entityIdentifierAddDevice(entityIdentifierAddDevice)
                .entityIdentifierDeleteDevice(entityIdentifierDeleteDevice)
                .flowExchangeDownHandler(flowExchangeDownHandler)
                .end()
                .initialEntities(()-> initialEntities.stream().map(entityConfig -> entityConfig.toEntity()).toList())
                .initialDevices(devices)
                .build();
    }
}
