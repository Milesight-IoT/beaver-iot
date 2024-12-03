package com.milesight.beaveriot.integration.service;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.Integration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class IntegrationServiceProviderImpl extends IntegrationServiceProvider {
    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Override
    public void save(Integration integrationConfig) {
        integrationConfig.getInitialDevices().forEach(deviceServiceProvider::save);
        entityServiceProvider.batchSave(integrationConfig.getInitialEntities());
    }

    @Override
    public void batchSave(Collection<Integration> integrationConfig) {
        integrationConfig.forEach(this::save);
    }
}
