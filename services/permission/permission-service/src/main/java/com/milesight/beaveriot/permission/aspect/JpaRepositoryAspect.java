package com.milesight.beaveriot.permission.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * author: Luxb
 * create: 2025/9/29 18:00
 **/
public class JpaRepositoryAspect {
    protected Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        var result = joinPoint.proceed();
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && joinPoint.getTarget() instanceof JpaRepository<?, ?> repository) {
            repository.flush();
        }
        return result;
    }
}
