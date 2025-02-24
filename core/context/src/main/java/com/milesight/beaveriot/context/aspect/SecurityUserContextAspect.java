package com.milesight.beaveriot.context.aspect;

import com.milesight.beaveriot.context.security.SecurityUser;
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
import org.springframework.util.ObjectUtils;

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
            SecurityUser securityUser = SecurityUser.builder().tenantId(tenantId).userId(parseLongSafely(userId)).build();
            com.milesight.beaveriot.context.security.SecurityUserContext.setSecurityUser(securityUser);
        }
    }

    private Long parseLongSafely(String value) {
        return ObjectUtils.isEmpty(value) ? null : Long.valueOf(value);
    }

    @After("pointCut()")
    public void after(JoinPoint joinPoint) {
        com.milesight.beaveriot.context.security.SecurityUserContext.clear();
    }

}
