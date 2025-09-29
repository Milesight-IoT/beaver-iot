package com.milesight.beaveriot.permission.support;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * author: Luxb
 * create: 2025/9/29 16:47
 **/
public class ProceedingJoinPointSupport {
    public static Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        var result = joinPoint.proceed();
        if (joinPoint.getTarget() instanceof JpaRepository<?, ?> repository) {
            repository.flush();
        }
        return result;
    }
}
