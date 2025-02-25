package net.javacrumbs.shedlock.spring.aop;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author leon
 */
public class ScopedLockApplicationContextInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        beanFactory.addBeanPostProcessor(new SmartInstantiationAwareBeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

                if (bean instanceof SpringLockConfigurationExtractor springLockConfigurationExtractor) {
                    return new ScopedSpringLockConfigurationExtractor(springLockConfigurationExtractor);
                } else if (bean instanceof MethodProxyScheduledLockAdvisor) {
                    return new ScopedLockMethodProxyScheduledLockAdvisor(beanFactory);
                }else {
                    return bean;
                }
            }
        });
    }
}
