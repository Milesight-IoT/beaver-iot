package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.permission.context.DataAspectContext;
import com.milesight.beaveriot.permission.util.TypeUtil;
import com.milesight.beaveriot.user.constants.UserConstants;
import jakarta.persistence.Table;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * @author loong
 * @date 2024/12/5 11:37
 */
@Component
@Aspect
@ConditionalOnClass(Pointcut.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Order
public class TenantAspect {

    @Pointcut("execution(* com.milesight.beaveriot..*Repository.*(..))")
    public void pointCut() {
    }

    @Around("pointCut()")
    @Transactional(rollbackFor = Exception.class)
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> repositoryInterface = joinPoint.getTarget().getClass().getInterfaces()[0];
        Tenant tenant = signature.getMethod().getAnnotation(Tenant.class);
        String tableName = null;
        if (tenant == null) {
            tenant = repositoryInterface.getAnnotation(Tenant.class);
        }
        if(Repository.class.isAssignableFrom(repositoryInterface)){
            Table annotation = AnnotationUtils.getAnnotation(((Class) TypeUtil.getTypeArgument(repositoryInterface, 0)), Table.class);
            tableName = annotation == null? null:annotation.name();
        }
        if (tableName == null) {
            return joinPoint.proceed();
        }
        if (tenant == null) {
            return joinPoint.proceed();
        }
        if (!tenant.enable()) {
            return joinPoint.proceed();
        }
        String columnName = tenant.column();
        if (columnName.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenant column name is not exist").build();
        }
        String tenantId = TenantContext.getTenantId();
        if(!StringUtils.hasText(tenantId)){
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
        }
        DataAspectContext.setTenantContext(tableName, DataAspectContext.TenantContext.builder()
                .tenantColumnName(columnName)
                .tenantId(tenantId)
                .build());
        try {
            return joinPoint.proceed();
        } finally {
            if (TransactionSynchronizationManager.isActualTransactionActive() &&
                    TransactionSynchronizationManager.isActualTransactionActive()) {
                // Register a synchronization to clear the context after the transaction completes
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        DataAspectContext.clearTenantContext();
                    }
                });
            } else {
                // If no transaction is active, clear the context immediately
                DataAspectContext.clearTenantContext();
            }
        }
    }

}
