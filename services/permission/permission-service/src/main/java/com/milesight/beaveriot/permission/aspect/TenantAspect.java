package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.TransactionUtils;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.permission.context.DataAspectContext;
import com.milesight.beaveriot.permission.helper.TenantValidationBypass;
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
public class TenantAspect extends JpaRepositoryAspect {

    @Pointcut("execution(* com.milesight.beaveriot..*Repository.*(..))")
    public void pointCut() {
    }

    @Around("pointCut()")
    @Transactional(rollbackFor = Exception.class)
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (TenantValidationBypass.isBypassed()) {
            return proceed(joinPoint);
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> repositoryInterface = joinPoint.getTarget().getClass().getInterfaces()[0];

        Tenant tenant = signature.getMethod().getAnnotation(Tenant.class);
        if (tenant == null) {
            tenant = repositoryInterface.getAnnotation(Tenant.class);
        }

        String tableName = RepositoryAspectUtils.getTableName(repositoryInterface);
        if (tableName == null || tenant == null || !tenant.enable()) {
            return proceed(joinPoint);
        }

        String columnName = tenant.column();
        if (columnName.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenant column name is not exist").build();
        }

        String tenantId = TenantContext.getTenantId();
        if(!StringUtils.hasText(tenantId)){
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not provided").build();
        }

        DataAspectContext.setTenantContext(tableName, DataAspectContext.TenantContext.builder()
                .tenantColumnName(columnName)
                .tenantId(tenantId)
                .build());

        try {
            return proceed(joinPoint);
        } finally {
            TransactionUtils.executeAfterCompletion(DataAspectContext::clearTenantContext);
        }
    }

}
