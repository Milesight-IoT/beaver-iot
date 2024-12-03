package com.milesight.beaveriot.context.configuration;

import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrapManager;
import com.milesight.beaveriot.context.integration.entity.EntityLoader;
import com.milesight.beaveriot.context.integration.entity.annotation.AnnotationEntityLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author leon
 */
@Configuration
public class IntegrationAutoConfiguration {

    @Bean(destroyMethod = "onDestroy")
    @ConditionalOnMissingBean
    public IntegrationBootstrapManager integrationBootstrapManager(ObjectProvider<EntityLoader> entityLoaders, ObjectProvider<IntegrationBootstrap> integrationBootstraps, IntegrationServiceProvider integrationStorageProvider) {
        return new IntegrationBootstrapManager(entityLoaders, integrationBootstraps, integrationStorageProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnnotationEntityLoader annotationEntityLoader() {
        return new AnnotationEntityLoader();
    }
}
