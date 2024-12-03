package com.milesight.beaveriot.permission.aspect;

import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.permission.dto.IntegrationPermissionDTO;
import com.milesight.beaveriot.permission.service.IntegrationPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/12/9 13:11
 */
@Slf4j
@Component
@Aspect
@ConditionalOnClass(Pointcut.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class IntegrationProviderAspect {

    @Autowired
    IntegrationPermissionService integrationPermissionService;

    @Pointcut("execution(* com.milesight.beaveriot..*IntegrationServiceProvider.get*(..)) || " +
            "execution(* com.milesight.beaveriot..*IntegrationServiceProvider.find*(..))")
    public void pointCut() {
    }

    @AfterReturning(pointcut = "pointCut()", returning = "result")
    public Object afterReturning(JoinPoint joinPoint, Object result) {
        if (result == null) {
            return null;
        }
        Long userId = SecurityUserContext.getUserId();
        if (userId == null){
            log.warn("user not login");
            return null;
        }
        IntegrationPermissionDTO integrationPermissionDTO = integrationPermissionService.getIntegrationPermission(userId);
        boolean hasAllPermission = integrationPermissionDTO.isHasAllPermission();
        if (hasAllPermission) {
            return result;
        }
        List<String> dataIds = integrationPermissionDTO.getIntegrationIds();
        if (result instanceof Integration) {
            Integration integration = (Integration) result;
            if (dataIds.contains(integration.getId())) {
                return integration;
            } else {
                return null;
            }
        } else if (result instanceof Collection) {
            if (((Collection<?>) result).isEmpty()) {
                return result;
            }
            Collection<?> collectionResult = (Collection<?>) result;
            return collectionResult.stream()
                    .filter(item -> item instanceof Integration && dataIds.contains(((Integration) item).getId()))
                    .collect(Collectors.toList());
        }
        return result;
    }

}
