package com.milesight.beaveriot.websocket;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author loong
 */
@Component
public class ChannelHandlerPrototypeBeanConfigurer implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String[] beanNames = registry.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (beanDefinition.getBeanClassName() != null
                    && isImplementingInterface(beanDefinition.getBeanClassName(), AbstractWebSocketHandler.class)) {
                beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            }
        }
    }

    private boolean isImplementingInterface(String className, Class<?> interfaceClass) {
        try {
            Class<?> clazz = Class.forName(className);
            return interfaceClass.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

}
