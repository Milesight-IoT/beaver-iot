package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.permission.context.DataAspectContext;
import com.milesight.beaveriot.permission.dto.PermissionDTO;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.permission.service.DashboardPermissionService;
import com.milesight.beaveriot.permission.service.DevicePermissionService;
import com.milesight.beaveriot.permission.service.EntityPermissionService;
import com.milesight.beaveriot.permission.service.WorkflowPermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author loong
 * @date 2024/12/5 12:00
 */
@Component
@Aspect
@ConditionalOnClass(Pointcut.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DataPermissionAspect {

    @Autowired
    EntityPermissionService entityPermissionService;
    @Autowired
    DashboardPermissionService dashboardPermissionService;
    @Autowired
    DevicePermissionService devicePermissionService;
    @Autowired
    WorkflowPermissionService workflowPermissionService;

    @Pointcut("execution(* com.milesight.beaveriot..*Repository.*(..))")
    public void pointCut() {
    }

    @Around("pointCut()")
    @Transactional(rollbackFor = Exception.class)
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> repositoryInterface = joinPoint.getTarget().getClass().getInterfaces()[0];

        DataPermission dataPermission = signature.getMethod().getAnnotation(DataPermission.class);
        if (dataPermission == null) {
            dataPermission = repositoryInterface.getAnnotation(DataPermission.class);
        }

        String tableName = RepositoryAspectUtils.getTableName(repositoryInterface);
        if (tableName == null || dataPermission == null) {
            return joinPoint.proceed();
        }

        DataPermissionType type = dataPermission.type();
        if (type == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("data permission type is not exist").build();
        }

        String columnName = dataPermission.column();
        if (columnName.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("data permission column name is not exist").build();
        }

        Long userId = SecurityUserContext.getUserId();
        if (userId == null) {
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not logged in").build();
        }

        PermissionDTO permissionDTO = switch (type) {
            case ENTITY -> entityPermissionService.getEntityPermission(userId);
            case DEVICE -> devicePermissionService.getDevicePermission(userId);
            case DASHBOARD -> dashboardPermissionService.getDashboardPermission(userId);
            case WORKFLOW -> workflowPermissionService.getWorkflowPermission(userId);
        };

        if (permissionDTO == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("unknown data permission type").build();
        }

        if (permissionDTO.isHaveAllPermissions()) {
            return joinPoint.proceed();
        }

        List<String> dataIds = permissionDTO.getIds();
        if (dataIds.isEmpty()) {
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user does not have data permission").build();
        }

        DataAspectContext.setDataPermissionContext(tableName, DataAspectContext.DataPermissionContext.builder()
                .dataIds(dataIds)
                .dataType(dataPermission.dataType())
                .dataColumnName(columnName)
                .build());

        try {
            return joinPoint.proceed();
        } finally {
            RepositoryAspectUtils.doAfterTransactionCompletion(DataAspectContext::clearDataPermissionContext);
        }
    }

}
