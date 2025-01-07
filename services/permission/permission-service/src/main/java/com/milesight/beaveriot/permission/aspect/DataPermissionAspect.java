package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.permission.context.DataAspectContext;
import com.milesight.beaveriot.permission.dto.DashboardPermissionDTO;
import com.milesight.beaveriot.permission.dto.DevicePermissionDTO;
import com.milesight.beaveriot.permission.dto.EntityPermissionDTO;
import com.milesight.beaveriot.permission.dto.WorkflowPermissionDTO;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.permission.service.DashboardPermissionService;
import com.milesight.beaveriot.permission.service.DevicePermissionService;
import com.milesight.beaveriot.permission.service.EntityPermissionService;
import com.milesight.beaveriot.permission.service.WorkflowPermissionService;
import com.milesight.beaveriot.permission.util.TypeUtil;
import jakarta.persistence.Table;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
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
        String tableName = null;
        if (dataPermission == null) {
            dataPermission = repositoryInterface.getAnnotation(DataPermission.class);
        }
        if(Repository.class.isAssignableFrom(repositoryInterface)){
            Table annotation = AnnotationUtils.getAnnotation(((Class) TypeUtil.getTypeArgument(repositoryInterface, 0)), Table.class);
            tableName = annotation==null?null:annotation.name();
        }
        if (tableName == null) {
            return joinPoint.proceed();
        }
        if (dataPermission == null) {
            return joinPoint.proceed();
        }
        DataPermissionType type = dataPermission.type();
        String columnName = dataPermission.column();
        if (type == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("data permission type is not exist").build();
        }
        if (columnName.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("data permission column name is not exist").build();
        }
        Long userId = SecurityUserContext.getUserId();
        if (userId == null) {
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not logged in").build();
        }
        boolean isHasAllPermission = false;
        List<Long> dataIds = new ArrayList<>();
        if(type == DataPermissionType.ENTITY) {
            EntityPermissionDTO entityPermissionDTO = entityPermissionService.getEntityPermission(userId);
            isHasAllPermission = entityPermissionDTO.isHasAllPermission();
            dataIds.addAll(entityPermissionDTO.getEntityIds().stream().map(Long::valueOf).toList());
        }else if(type == DataPermissionType.DEVICE) {
            DevicePermissionDTO devicePermissionDTO = devicePermissionService.getDevicePermission(userId);
            isHasAllPermission = devicePermissionDTO.isHasAllPermission();
            dataIds.addAll(devicePermissionDTO.getDeviceIds().stream().map(Long::valueOf).toList());
        }else if (type == DataPermissionType.DASHBOARD) {
            DashboardPermissionDTO dashboardPermissionDTO = dashboardPermissionService.getDashboardPermission(userId);
            isHasAllPermission = dashboardPermissionDTO.isHasAllPermission();
            dataIds.addAll(dashboardPermissionDTO.getDashboardIds().stream().map(Long::valueOf).toList());
        }else if(type == DataPermissionType.WORKFLOW) {
            WorkflowPermissionDTO workflowPermissionDTO = workflowPermissionService.getWorkflowPermission(userId);
            isHasAllPermission = workflowPermissionDTO.isHasAllPermission();
            dataIds.addAll(workflowPermissionDTO.getWorkflowIds().stream().map(Long::valueOf).toList());
        }else {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("unknown data permission type").build();
        }
        if(isHasAllPermission){
            return joinPoint.proceed();
        }
        if(dataIds.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("user does not have data permission").build();
        }
        DataAspectContext.setDataPermissionContext(tableName, DataAspectContext.DataPermissionContext.builder()
                .dataIds(dataIds)
                .dataColumnName(columnName)
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
                        DataAspectContext.clearDataPermissionContext();
                    }
                });
            } else {
                // If no transaction is active, clear the context immediately
                DataAspectContext.clearDataPermissionContext();
            }
        }
    }

}
