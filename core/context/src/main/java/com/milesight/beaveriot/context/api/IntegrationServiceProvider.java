package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrapManager;
import com.milesight.beaveriot.context.integration.model.Integration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author leon
 */
public abstract class IntegrationServiceProvider {

    @Lazy
    @Autowired
    private IntegrationBootstrapManager integrationBootstrapManager;

    public abstract void save(Integration integrationConfig);

    public abstract void batchSave(Collection<Integration> integrationConfig);

    public Integration getIntegration(String integrationId) {
        return integrationBootstrapManager.getIntegrationContext().getIntegration(integrationId);
    }

    public Integration getActiveIntegration(String integrationId) {
        Integration integration = integrationBootstrapManager.getIntegrationContext().getIntegration(integrationId);
        return integration != null && integration.isEnabled() ? integration : null;
    }

    public Collection<Integration> findIntegrations() {
        return integrationBootstrapManager.getIntegrationContext().getAllIntegrations().values();
    }

    public List<Integration> findVisibleIntegrations() {
        return integrationBootstrapManager.getIntegrationContext().getAllIntegrations().values().stream().filter(Integration::isVisible).toList();
    }

    public List<Integration> findActiveIntegrations() {
        return integrationBootstrapManager.getIntegrationContext().getAllIntegrations().values().stream().filter(Integration::isEnabled).toList();
    }

    public List<Integration> findIntegrations(Predicate<Integration> predicate) {
        return integrationBootstrapManager.getIntegrationContext().getAllIntegrations().values().stream().filter(predicate).toList();
    }

}
