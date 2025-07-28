package com.milesight.beaveriot.semaphore.aspect;

import com.google.common.collect.Sets;
import com.milesight.beaveriot.base.annotations.semaphore.DistributedSemaphore;
import com.milesight.beaveriot.base.annotations.semaphore.SemaphoreScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * author: Luxb
 * create: 2025/7/28 15:29
 **/
@Component
@Slf4j
public class DistributedSemaphoreInitializer implements SmartInitializingSingleton {
    private final ApplicationContext applicationContext;
    private final DistributedSemaphoreAspect distributedSemaphoreAspect;
    private final IUserFacade userFacade;
    private final Set<String> semaphoreKeys;

    public DistributedSemaphoreInitializer(ApplicationContext applicationContext, DistributedSemaphoreAspect distributedSemaphoreAspect, IUserFacade userFacade) {
        this.applicationContext = applicationContext;
        this.distributedSemaphoreAspect = distributedSemaphoreAspect;
        this.userFacade = userFacade;
        this.semaphoreKeys = Sets.newHashSet();
    }

    @Override
    public void afterSingletonsInstantiated() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
        List<TenantDTO> tenants = userFacade.getAllTenants();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());

            for (Method method : methods) {
                DistributedSemaphore distributedSemaphore = method.getAnnotation(DistributedSemaphore.class);
                if (distributedSemaphore != null) {
                    initDistributedSemaphore(distributedSemaphore.name(), distributedSemaphore.permits(), tenants, distributedSemaphore.scope());
                }
            }
        }
    }

    private void initDistributedSemaphore(String name, int permits, List<TenantDTO> tenants, SemaphoreScope scope) {
        if (scope == SemaphoreScope.GLOBAL) {
            String key = DistributedSemaphoreAspect.getKey(name);
            checkSemaphoreKey(key);
            distributedSemaphoreAspect.initDistributedSemaphore(key, permits);
        } else {
            for (TenantDTO tenant : tenants) {
                String key = DistributedSemaphoreAspect.getKey(name, tenant.getTenantId());
                checkSemaphoreKey(key);
                distributedSemaphoreAspect.initDistributedSemaphore(key, permits);
            }
        }
    }

    private void checkSemaphoreKey(String key) {
        if (semaphoreKeys.contains(key)) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Duplicated semaphore key: " + key)
                    .build();
        }
        semaphoreKeys.add(key);
    }
}
