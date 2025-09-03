package com.milesight.beaveriot.context.configuration;

import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.api.ResourceFingerprintServiceProvider;
import com.milesight.beaveriot.context.api.TenantServiceProvider;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrapManager;
import com.milesight.beaveriot.context.integration.entity.EntityLoader;
import com.milesight.beaveriot.context.integration.entity.annotation.AnnotationEntityLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author leon
 */
@Configuration
public class IntegrationAutoConfiguration {

    @Bean(destroyMethod = "onDestroy")
    @ConditionalOnMissingBean
    public IntegrationBootstrapManager integrationBootstrapManager(ObjectProvider<EntityLoader> entityLoaders,
                                                                   ObjectProvider<IntegrationBootstrap> integrationBootstraps,
                                                                   IntegrationServiceProvider integrationStorageProvider,
                                                                   Environment environment,
                                                                   TenantServiceProvider tenantServiceProvider,
                                                                   EntityTemplateServiceProvider entityTemplateServiceProvider,
                                                                   ResourceFingerprintServiceProvider resourceFingerprintServiceProvider) {
        return new IntegrationBootstrapManager(entityLoaders,
                integrationBootstraps,
                integrationStorageProvider,
                environment,
                tenantServiceProvider,
                entityTemplateServiceProvider,
                resourceFingerprintServiceProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnnotationEntityLoader annotationEntityLoader() {
        return new AnnotationEntityLoader();
    }

    @Bean
    @ConditionalOnBean(ThreadPoolTaskExecutorBuilder.class)
    public ThreadPoolTaskExecutor integrationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        return builder.build();
    }
}
