package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.user.repository.TenantRepository;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;

/**
 * @author leon
 */
@Order(3)
@Slf4j
@Service
public class IntegrationAuthorizedService implements CommandLineRunner {

    @Autowired
    private IntegrationServiceProvider integrationServiceProvider;
    @Autowired
    private TenantRepository tenantRepository;

    public void authorize(String tenantId, List<String> integrationIds) {

        Assert.notNull(tenantId, "tenantId must not be null");
        Assert.notNull(integrationIds, "integrationIds must not be null");

        log.debug("Authorizing integrations for tenant: {}, integrationIds: {}", tenantId, integrationIds);

        TenantContext.setTenantId(tenantId);

        List<Integration> integrations = integrationIds.stream().map(integrationServiceProvider::getIntegration).toList();
        integrationServiceProvider.batchSave(integrations);

        integrations.forEach(integration -> {
            try {
                if (integration.getIntegrationClass() != null) {
                    SpringContext.getBean(integration.getIntegrationClass()).onEnabled(tenantId, integration);
                }
            } catch (Exception e) {
                log.error("loading integration {} error: {}", integration.getId(), e.getMessage());
            }
        });
    }


    @Override
    public void run(String... args) throws Exception {
        //todo: remove
        Collection<Integration> integrations = integrationServiceProvider.findActiveIntegrations();
        List<String> integrationIds = integrations.stream().map(Integration::getId).toList();

        tenantRepository.findAll().forEach(tenant -> {
            authorize(tenant.getId(), integrationIds);
        });
    }
}
