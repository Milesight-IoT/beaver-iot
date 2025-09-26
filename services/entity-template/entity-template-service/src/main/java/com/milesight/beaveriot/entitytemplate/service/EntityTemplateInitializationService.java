package com.milesight.beaveriot.entitytemplate.service;

import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.entity.EntityTemplateConfig;
import com.milesight.beaveriot.resource.manager.facade.IResourceFingerprintFacade;
import com.milesight.beaveriot.resource.manager.model.ResourceFingerprint;
import com.milesight.beaveriot.resource.manager.model.ResourceFingerprintType;
import com.milesight.beaveriot.user.facade.ITenantFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * author: Luxb
 * create: 2025/9/12 13:59
 **/
@Slf4j
@Service
@Order(0)
public class EntityTemplateInitializationService implements CommandLineRunner {
    private final Environment environment;
    private final ITenantFacade tenantFacade;
    private final IResourceFingerprintFacade resourceFingerprintFacade;
    private final EntityTemplateService entityTemplateService;

    public EntityTemplateInitializationService(Environment environment, ITenantFacade tenantFacade, IResourceFingerprintFacade resourceFingerprintFacade, EntityTemplateService entityTemplateService) {
        this.environment = environment;
        this.tenantFacade = tenantFacade;
        this.resourceFingerprintFacade = resourceFingerprintFacade;
        this.entityTemplateService = entityTemplateService;
    }

    @Override
    public void run(String... args) {
        // Initialize entity templates before starting integrations
        initializeEntityTemplates();
        log.info("Entity templates initialized successfully");
    }

    private void initializeEntityTemplates() {
        EntityTemplateConfig entityTemplateConfig = Binder.get(environment).bind(EntityTemplateConfig.PROPERTY_PREFIX, EntityTemplateConfig.class).orElse(null);
        if (entityTemplateConfig == null) {
            return;
        }

        String hash = com.milesight.beaveriot.base.utils.ObjectUtils.md5Sum(entityTemplateConfig.getInitialEntityTemplates());
        if (hash == null) {
            return;
        }

        ResourceFingerprint resourceFingerprint = resourceFingerprintFacade.getResourceFingerprint(ResourceFingerprintType.TYPE_ENTITY_TEMPLATE, IntegrationConstants.SYSTEM_INTEGRATION_ID);
        if (resourceFingerprint == null) {
            resourceFingerprint = ResourceFingerprint.builder()
                    .type(ResourceFingerprintType.TYPE_ENTITY_TEMPLATE)
                    .integration(IntegrationConstants.SYSTEM_INTEGRATION_ID)
                    .build();
        }

        if (!hash.equals(resourceFingerprint.getHash())) {
            tenantFacade.runWithAllTenants(() ->
                    entityTemplateService.batchSave(entityTemplateConfig.getInitialEntityTemplates())
            );
            resourceFingerprint.setHash(hash);
            resourceFingerprintFacade.save(resourceFingerprint);
        }
    }
}
