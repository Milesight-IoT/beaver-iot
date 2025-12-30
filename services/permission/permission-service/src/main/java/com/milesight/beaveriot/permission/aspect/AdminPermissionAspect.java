package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.permission.service.PermissionService;
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

/**
 * @author luxb
 * @date 2025/12/30 17:34
 */
@Component
@Aspect
@ConditionalOnClass(Pointcut.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AdminPermissionAspect {

    @Autowired
    IUserFacade userFacade;

    @Autowired
    PermissionService permissionService;

    @Pointcut("@annotation(AdminPermission)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void checkAdminPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AdminPermission adminPermission = signature.getMethod().getAnnotation(AdminPermission.class);
        if (adminPermission != null) {
            permissionService.checkAdminPermission();
        }
    }
}
