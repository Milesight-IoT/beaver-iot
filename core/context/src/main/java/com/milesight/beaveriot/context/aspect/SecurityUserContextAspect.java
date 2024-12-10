package com.milesight.beaveriot.context.aspect;

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
public class SecurityUserContextAspect {

    @Pointcut("@annotation(SecurityUserContext)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        SecurityUserContext securityUserContext = signature.getMethod().getAnnotation(SecurityUserContext.class);
        if (securityUserContext != null) {
            String tenantId = AnnotationSpelExpressionUtil.getSpelKeyValue(joinPoint, securityUserContext.tenantId());
            String userId = AnnotationSpelExpressionUtil.getSpelKeyValue(joinPoint, securityUserContext.userId());
            Map<String, Object> payload = Map.of(
                    com.milesight.beaveriot.context.security.SecurityUserContext.TENANT_ID, tenantId,
                    com.milesight.beaveriot.context.security.SecurityUserContext.USER_ID, userId
            );
            com.milesight.beaveriot.context.security.SecurityUserContext.SecurityUser securityUser = com.milesight.beaveriot.context.security.SecurityUserContext.SecurityUser.builder()
                    .payload(payload)
                    .build();
            com.milesight.beaveriot.context.security.SecurityUserContext.setSecurityUser(securityUser);
        }
    }

    @After("pointCut()")
    public void after(JoinPoint joinPoint) {
        com.milesight.beaveriot.context.security.SecurityUserContext.clear();
    }

}
