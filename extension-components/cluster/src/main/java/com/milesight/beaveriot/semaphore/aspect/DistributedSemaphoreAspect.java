package com.milesight.beaveriot.semaphore.aspect;

import com.milesight.beaveriot.base.annotations.semaphore.SemaphoreScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.semaphore.DistributedSemaphore;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/7/28 15:43
 **/
@Slf4j
@Aspect
@Component
public class DistributedSemaphoreAspect {
    private final DistributedSemaphore distributedSemaphore;
    public static final String SEMAPHORE_KEY_FORMAT_TENANT = "semaphore:{0}:{1}";
    public static final String SEMAPHORE_KEY_FORMAT_GLOBAL = "semaphore:{0}";

    public DistributedSemaphoreAspect(DistributedSemaphore distributedSemaphore) {
        this.distributedSemaphore = distributedSemaphore;
    }

    public static String getKey(String name, String tenantId) {
        return MessageFormat.format(SEMAPHORE_KEY_FORMAT_TENANT, tenantId, name);
    }

    public static String getKey(String name) {
        return MessageFormat.format(SEMAPHORE_KEY_FORMAT_GLOBAL, name);
    }

    @Pointcut("@annotation(com.milesight.beaveriot.base.annotations.semaphore.DistributedSemaphore)")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.milesight.beaveriot.base.annotations.semaphore.DistributedSemaphore distributedSemaphore = method.getAnnotation(com.milesight.beaveriot.base.annotations.semaphore.DistributedSemaphore.class);
        String key;
        if (distributedSemaphore.scope() == SemaphoreScope.GLOBAL) {
            key = getKey(distributedSemaphore.name());
        } else {
            key = getKey(distributedSemaphore.name(), TenantContext.getTenantId());
        }
        Object result;
        String permitId = this.distributedSemaphore.acquire(key, Duration.ofMillis(distributedSemaphore.timeout()));
        if (permitId != null) {
            try {
                result = joinPoint.proceed();
            } finally {
                this.distributedSemaphore.release(key, permitId);
            }
        } else {
            String errorMessage = MessageFormat.format("Failed occurred during acquiring semaphore {0}", key);
            log.error(errorMessage);
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), errorMessage).build();
        }
        return result;
    }

    public void initDistributedSemaphore(String key, int permits) {
        distributedSemaphore.initPermits(key, permits);
    }
}
