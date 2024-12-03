package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.permission.enums.MenuCodeEnum;
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

import java.util.List;

/**
 * @author loong
 * @date 2024/12/3 17:21
 */
@Component
@Aspect
@ConditionalOnClass(Pointcut.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class MenuPermissionAspect {

    @Autowired
    IUserFacade userFacade;

    @Pointcut("@annotation(MenuPermission)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void checkMenuPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MenuPermission menuPermission = signature.getMethod().getAnnotation(MenuPermission.class);
        if (menuPermission != null) {
            MenuCodeEnum code = menuPermission.code();
            if (code != null) {
                Long userId = SecurityUserContext.getUserId();
                if (userId == null) {
                    throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not login").build();
                }
                List<MenuDTO> menuDTOList = userFacade.getMenusByUserId(userId);
                if (menuDTOList == null || menuDTOList.isEmpty()) {
                    throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not have permission").build();
                }
                boolean hasPermission = menuDTOList.stream().anyMatch(menuDTO -> menuDTO.getMenuCode().equals(code.name()));
                if (!hasPermission) {
                    throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not have permission").build();
                }
            }
        }
    }
}
