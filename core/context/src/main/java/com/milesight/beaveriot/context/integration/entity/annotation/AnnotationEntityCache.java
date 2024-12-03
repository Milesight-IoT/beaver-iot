package com.milesight.beaveriot.context.integration.entity.annotation;

import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.model.Entity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leon
 */
@Slf4j
public enum AnnotationEntityCache {

    INSTANCE;

    private Map<Class, Map<Method, String>> entitiesMethodCache = new ConcurrentHashMap<>();

    private Map<Class<?>, List<Entity>> deviceTemplateEntitiesCache = new ConcurrentHashMap<>();

    public void cacheDeviceTemplateEntities(Class<?> clazz, List<Entity> entities) {
        if (ObjectUtils.isEmpty(entities)) {
            return;
        }
        if (deviceTemplateEntitiesCache.containsKey(clazz)) {
            deviceTemplateEntitiesCache.get(clazz).addAll(entities);
        } else {
            deviceTemplateEntitiesCache.put(clazz, entities);
        }
    }

    public List<Entity> getDeviceTemplateEntities(Class<?> clazz) {
        return deviceTemplateEntitiesCache.get(clazz);
    }

    public void cacheEntityMethod(Field filed, String key) {
        Method getterMethod = getGetterMethod(filed.getDeclaringClass(), filed);
        if (getterMethod != null) {
            Map<Method, String> clazzMethodCache = entitiesMethodCache.computeIfAbsent(filed.getDeclaringClass(), k -> new ConcurrentHashMap<>());
            clazzMethodCache.put(getterMethod, key);
        }

        Method setterMethod = getSetterMethod(filed.getDeclaringClass(), filed);
        if(setterMethod != null){
            Map<Method, String> clazzMethodCache = entitiesMethodCache.computeIfAbsent(filed.getDeclaringClass(), k -> new ConcurrentHashMap<>());
            clazzMethodCache.put(setterMethod, key);
        }
    }

    public String getEntityKeyByMethod(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        return entitiesMethodCache.containsKey(declaringClass) ? entitiesMethodCache.get(declaringClass).get(method) : null;
    }

    private Method getGetterMethod(Class<?> clazz, Field field) {
        String fieldName = field.getName();
        String capitalizedFieldName = StringUtils.upperFirst(fieldName);
        String getterName = "get" + capitalizedFieldName;
        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
                try {
                    String booleanGetterName = "is" + capitalizedFieldName;
                    return clazz.getMethod(booleanGetterName);
                } catch (NoSuchMethodException ex) {
                    log.info("No getter method found for field :" + fieldName + ",This cannot be used to receive ExchangePayload data");
                }
            }
            log.info("No getter method found for field :" + fieldName + ",This cannot be used to receive ExchangePayload data");
        }
        return null;
    }

    private Method getSetterMethod(Class<?> clazz, Field field) {
        String fieldName = field.getName();
        String setterName = "set" + StringUtils.upperFirst(fieldName);
        try {
            return clazz.getMethod(setterName, field.getType());
        } catch (NoSuchMethodException e) {
            log.debug("No setter method found for field :" + fieldName + ",This cannot be used to set ExchangePayloadProxy data");
        }
        return null;
    }
}
