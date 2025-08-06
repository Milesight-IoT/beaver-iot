package com.milesight.beaveriot.context.i18n.injector;

import com.milesight.beaveriot.context.i18n.support.MessageResolverBeanSupporter;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.support.PackagesScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/8/5 17:37
 **/
@Component
public class IntegrationAutowiredMessageResolverInjector {
    private final ApplicationContext applicationContext;
    private final MessageResolverBeanSupporter messageResolverBeanSupporter;
    private final PackagesScanner packagesScanner;

    public IntegrationAutowiredMessageResolverInjector(ApplicationContext applicationContext, MessageResolverBeanSupporter messageResolverBeanSupporter) {
        this.applicationContext = applicationContext;
        this.messageResolverBeanSupporter = messageResolverBeanSupporter;
        this.packagesScanner = new PackagesScanner();
    }

    public void injectMessageResolver(Integration integration) {
        packagesScanner.doScan(integration.getIntegrationClass().getPackage().getName(), clazz -> {
            Object bean = getBean(clazz);
            if (bean != null) {
                messageResolverBeanSupporter.injectMessageResolvers(bean, clazz, integration.getId());
            }
        });
    }

    private Object getBean(Class<?> clazz) {
        try {
            return applicationContext.getBean(clazz);
        } catch (Exception e) {
            return null;
        }
    }
}