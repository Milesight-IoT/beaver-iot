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
        DataPermission dataPermission = signature.getMethod().getAnnotation(DataPermission.class);
        if (dataPermission == null) {
            Class<?> repositoryInterface = joinPoint.getTarget().getClass().getInterfaces()[0];
            dataPermission = repositoryInterface.getAnnotation(DataPermission.class);
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
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not login").build();
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
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("user not have entity permission").build();
        }
        DataAspectContext.setDataPermissionContext(DataAspectContext.DataPermissionContext.builder()
                .dataIds(dataIds)
                .dataColumnName(columnName)
                .build());
        try {
            return joinPoint.proceed();
        } finally {
            DataAspectContext.clearDataPermissionContext();
        }
    }

}
