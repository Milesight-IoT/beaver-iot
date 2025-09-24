package com.milesight.beaveriot.context.integration.bootstrap;

import com.milesight.beaveriot.base.exception.BootstrapException;
import com.milesight.beaveriot.base.exception.ConfigurationException;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.IntegrationContext;
import com.milesight.beaveriot.context.integration.entity.EntityLoader;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.model.config.IntegrationConfig;
import com.milesight.beaveriot.context.support.YamlPropertySourceFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author leon
 */
@Slf4j
@Order(2)
public class IntegrationBootstrapManager implements CommandLineRunner {

    private final YamlPropertySourceFactory propertySourceFactory;
    private final IntegrationContext integrationContext = new IntegrationContext();
    private final ObjectProvider<EntityLoader> entityLoaders;
    private final ObjectProvider<IntegrationBootstrap> integrationBootstrapList;

    public IntegrationBootstrapManager(ObjectProvider<EntityLoader> entityLoaders,
                                       ObjectProvider<IntegrationBootstrap> integrationBootstraps) {
        this.entityLoaders = entityLoaders;
        this.integrationBootstrapList = integrationBootstraps;
        this.propertySourceFactory = new YamlPropertySourceFactory();
    }

    public void onStarted() {
        // Add default integration: "system"
        integrationContext.cacheIntegration(
                new Integration(IntegrationConstants.SYSTEM_INTEGRATION_ID, IntegrationConstants.SYSTEM_INTEGRATION_ID, false),
                new SystemIntegrationBootstrap(), new StandardEnvironment());

        integrationBootstrapList.orderedStream().forEach(integrationBootstrap -> {
            try {
                long currentTimeMillis = System.currentTimeMillis();

                PropertySource<?> integrationPropertySource = loadIntegrationPropertySource(integrationBootstrap);

                StandardEnvironment integrationEnvironment = createIntegrationEnvironment(integrationPropertySource);

                Integration integration = buildIntegrationConfig(integrationBootstrap.getClass(), integrationEnvironment);

                loadIntegrationEntityConfig(integration, integrationEnvironment);

                integrationBootstrap.onPrepared(integration);

                integration.initializeProperties();

                integrationContext.cacheIntegration(integration, integrationBootstrap, integrationEnvironment);

                int allDeviceEntitySize = integration.getInitialDevices().stream().mapToInt(device -> ObjectUtils.isEmpty(device.getEntities()) ? 0 : device.getEntities().size()).sum();

                long cost = System.currentTimeMillis() - currentTimeMillis;

                log.debug("Integration {} started, Contains device size {}, device entity size {}, and integrated entity size {}, cost : {}", integration.getName(), integration.getInitialDevices().size(), allDeviceEntitySize, integration.getInitialEntities().size(), cost);
            } catch (Exception e) {
                log.error("Failed to load integration yaml", e);
            }
        });

//        integrationStorageProvider.batchSave(integrationContext.getAllIntegrations().values());

        integrationBootstrapList.orderedStream().forEach(integrationBootstrap -> {
            try {
                Integration integration = integrationContext.getIntegration(integrationBootstrap);
                if (integration != null) {
                    integrationBootstrap.onStarted(integration);
                }
            } catch (Exception ex) {
                log.error("Failed to start integration yaml", ex);
            }
        });

        log.info("IntegrationBootstrapManager started, contains integrations : {}", integrationContext.getAllIntegrations().keySet());
    }

    public IntegrationContext getIntegrationContext() {
        return integrationContext;
    }

    @SneakyThrows
    private PropertySource<?> loadIntegrationPropertySource(IntegrationBootstrap integrationBootstrap) {
        try {
            String path = integrationBootstrap.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            return propertySourceFactory.createJarPropertySource(integrationBootstrap.getClass().getSimpleName(), path);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load integration yaml", e);
        }
    }

    public void onDestroy() {
        Map<String, Integration> allIntegrationConfigs = integrationContext.getAllIntegrations();
        integrationContext.getAllIntegrationBootstraps().entrySet().forEach(integrationBootstrapEntry -> {
            Integration integrationConfig = allIntegrationConfigs.get(integrationBootstrapEntry.getKey());
            integrationBootstrapEntry.getValue().onDestroy(integrationConfig);
        });
    }

    public void onEnabled(String tenantId, String integrationName) {
        Integration integrationConfig = integrationContext.getIntegration(integrationName);
        Assert.notNull(integrationConfig, "Integration config not found");
        Assert.notNull(tenantId, "TenantId can not be null");
        integrationConfig.setEnabled(false);
        integrationContext.getIntegrationBootstrap(integrationName).onEnabled(tenantId, integrationConfig);
    }

    public void onDisabled(String tenantId, String integrationName) {
        Integration integrationConfig = integrationContext.getIntegration(integrationName);
        Assert.notNull(integrationConfig, "Integration config not found");
        Assert.notNull(tenantId, "TenantId can not be null");
        integrationConfig.setEnabled(true);
        integrationContext.getIntegrationBootstrap(integrationName).onDisabled(tenantId, integrationConfig);
    }

    private StandardEnvironment createIntegrationEnvironment(PropertySource<?> integrationPropertySource) {
        return new StandardEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                propertySources.addFirst(integrationPropertySource);
            }
        };
    }

    @SneakyThrows
    private Integration buildIntegrationConfig(Class<? extends IntegrationBootstrap> clazz, StandardEnvironment environment) {
        BindResult<HashMap> integrationRoot = Binder.get(environment).bind(IntegrationConstants.INTEGRATION_PROPERTY_PREFIX, HashMap.class);
        if (!(integrationRoot.isBound() && integrationRoot.get() instanceof Map)) {
            throw new ConfigurationException("Integration information not configured, please check integration.yaml");
        }
        if (integrationRoot.get().size() != 1) {
            throw new ConfigurationException("Integration information not configured correctly, There is one and only one integration configuration, please check integration.yaml");
        }
        String integrationId = (String) integrationRoot.get().keySet().iterator().next();
        IntegrationConfig integrationConfig = Binder.get(environment).bind(IntegrationConstants.INTEGRATION_PROPERTY_PREFIX + "." + integrationId, IntegrationConfig.class).get();
        Integration integration = integrationConfig.toIntegration(integrationId);
        integration.setIntegrationClass(clazz);
        integration.setEnvironment(environment);
        return integration;
    }

    private void loadIntegrationEntityConfig(Integration integration, StandardEnvironment integrationEnvironment) {
        entityLoaders.stream().forEach(entityLoader -> {
            try {
                entityLoader.load(integration, integrationEnvironment);
            } catch (Exception e) {
                throw new BootstrapException("Failed to load entity config", e);
            }
        });
    }

    @Override
    public void run(String... args) throws Exception {
        onStarted();
    }
}

