package com.milesight.beaveriot.cluster;

import com.milesight.beaveriot.base.cluster.ClusterAware;
import com.milesight.beaveriot.base.cluster.ClusterValidation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import java.util.Map;

public class ClusterValidationInitializingBean implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private ClusterProperties clusterProperties;

    public ClusterValidationInitializingBean(ClusterProperties clusterProperties) {
        this.clusterProperties = clusterProperties;
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!clusterProperties.isEnabled()) {
            return;
        }

        String[] beanNamesForAnnotation = applicationContext.getBeanNamesForAnnotation(ClusterValidation.class);
        for (String beanName : beanNamesForAnnotation) {
            Class<?> beanClass = applicationContext.getType(beanName);
            ClusterValidation annotation = AnnotationUtils.getAnnotation(beanClass,ClusterValidation.class);
            validateBeans(annotation.requiredBeans(), annotation.unsupportedBeans());
        }

        Map<String, ClusterAware> clusterAwareBeans = applicationContext.getBeansOfType(ClusterAware.class);
        clusterAwareBeans.forEach((beanName, bean) -> {
            Assert.isTrue(bean.isClusterSupported(), beanName + " bean is necessary to support running in a cluster environment, please check the cluster.enabled configuration");
        });
    }

    private void validateBeans(Class<?>[] requiredBeans, Class<?>[] forbiddenBeans) {
        for (Class<?> beanClazz : requiredBeans) {
            if (!isBeanPresent(beanClazz)) {
                throw new IllegalStateException("Required bean not found: " + beanClazz);
            }
        }
        for (Class<?> beanName : forbiddenBeans) {
            if (isBeanPresent(beanName)) {
                throw new IllegalStateException("Unsupported bean found: " + beanName);
            }
        }
    }

    public boolean isBeanPresent(Class<?> beanClass) {
        return applicationContext.getBeanNamesForType(beanClass).length > 0;
    }

}