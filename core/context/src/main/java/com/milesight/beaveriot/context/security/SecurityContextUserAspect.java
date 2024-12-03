package com.milesight.beaveriot.context.security;

import com.milesight.beaveriot.context.util.AnnotationSpelExpressionUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author loong
 * @date 2024/12/5 13:00
 */
@Component
@Aspect
@ConditionalOnClass(Pointcut.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class SecurityContextUserAspect {

    @Pointcut("@annotation(SecurityContextUser)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        SecurityContextUser securityContextUser = signature.getMethod().getAnnotation(SecurityContextUser.class);
        if (securityContextUser != null) {
            SecurityUserContext.SecurityUser securityUser = SecurityUserContext.getSecurityUser();
            if(securityUser == null) {
                String tenantId = AnnotationSpelExpressionUtil.getSpelKeyValue(joinPoint, securityContextUser.tenantId());
                Map<String, Object> payload = Map.of(
                        SecurityUserContext.TENANT_ID, tenantId
                );
                securityUser = SecurityUserContext.SecurityUser.builder()
                        .payload(payload)
                        .build();
                SecurityUserContext.setSecurityUser(securityUser);
            }
        }
    }

    @After("pointCut()")
    public void after(JoinPoint joinPoint) {
        SecurityUserContext.clear();
    }

}
