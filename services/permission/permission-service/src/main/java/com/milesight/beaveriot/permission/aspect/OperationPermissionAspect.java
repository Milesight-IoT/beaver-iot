package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.user.dto.MenuDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author loong
 * @date 2024/12/3 17:21
 */
@Component
@Aspect
@ConditionalOnClass(Pointcut.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class OperationPermissionAspect {

    @Autowired
    IUserFacade userFacade;

    @Pointcut("@annotation(OperationPermission)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void checkMenuPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationPermission operationPermission = signature.getMethod().getAnnotation(OperationPermission.class);
        if (operationPermission != null) {
            OperationPermissionCode[] codes = operationPermission.codes();
            if (codes != null && codes.length > 0) {
                Long userId = SecurityUserContext.getUserId();
                if (userId == null) {
                    throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not login").build();
                }
                List<MenuDTO> menuDTOList = userFacade.getMenusByUserId(userId);
                if (menuDTOList == null || menuDTOList.isEmpty()) {
                    throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not have permission").build();
                }
                List<String> operationPermissionCodes = Arrays.stream(codes).map(OperationPermissionCode::getCode).toList();
                boolean hasPermission = menuDTOList.stream().anyMatch(menuDTO -> operationPermissionCodes.contains(menuDTO.getMenuCode()));
                if (!hasPermission) {
                    throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not have permission").build();
                }
            }
        }
    }
}
