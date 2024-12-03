package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.permission.context.DataAspectContext;
import com.milesight.beaveriot.user.constants.UserConstants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        Tenant tenant = signature.getMethod().getAnnotation(Tenant.class);
        if (tenant == null) {
            Class<?> repositoryInterface = joinPoint.getTarget().getClass().getInterfaces()[0];
            tenant = repositoryInterface.getAnnotation(Tenant.class);
        }
        if (tenant != null && !tenant.enable()) {
            return joinPoint.proceed();
        }
        String columnName = SecurityUserContext.TENANT_ID;
        if (tenant != null) {
            columnName = tenant.column();
        }
        if (columnName.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenant column name is not exist").build();
        }
        //TODO Ensure the presence of tenantId in the context
        Long tenantId = SecurityUserContext.getTenantId() == null ? UserConstants.DEFAULT_TENANT_ID : SecurityUserContext.getTenantId();
        if(tenantId == null){
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
        }
        DataAspectContext.setTenantContext(DataAspectContext.TenantContext.builder()
                .tenantColumnName(columnName)
                .tenantId(tenantId)
                .build());
        try {
            return joinPoint.proceed();
        } finally {
            DataAspectContext.clearTenantContext();
        }
    }

}
