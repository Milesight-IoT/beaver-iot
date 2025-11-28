package com.milesight.beaveriot.base.pool.component;

import com.milesight.beaveriot.base.pool.ObjectPool;
import com.milesight.beaveriot.base.pool.PoolConfig;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Luxb
 * @date 2025/11/27 14:04
 **/
@Component
public class ObjectPoolManager {
    private final ConfigurableApplicationContext applicationContext;

    public ObjectPoolManager(ApplicationContext applicationContext) {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @SuppressWarnings("unused")
    public <T> ObjectPool<T> getPool(PoolConfig config,
                                     Supplier<T> objectFactory,
                                     Class<T> clazz) {
        return getPool(config, objectFactory, null, clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectPool<T> getPool(PoolConfig config,
                                     Supplier<T> objectFactory,
                                     Consumer<T> objectDestructor,
                                     Class<T> objectClass) {
        String beanName = getBeanName(objectClass.getName());
        if (!applicationContext.containsBean(beanName)) {
            ObjectPool<T> pool = ObjectPool.newPool(config, objectFactory, objectDestructor, objectClass);
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition((Class<ObjectPool<T>>) pool.getClass(), () -> pool);
            beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
        }
        return applicationContext.getBean(beanName, ObjectPool.class);
    }

    private String getBeanName(String className) {
        return Constants.OBJECT_POOL_BEAN_NAME_PREFIX + className;
    }

    private static class Constants {
        public static final String OBJECT_POOL_BEAN_NAME_PREFIX = "object-pool:";
    }
}
