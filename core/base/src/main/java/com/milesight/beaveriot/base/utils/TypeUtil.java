package com.milesight.beaveriot.base.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author loong
 * @date 2024/12/18 18:04
 */
public class TypeUtil {

    public static Type getTypeArgument(Type type, int index) {
        final Type[] typeArguments = getTypeArguments(type);
        if (null != typeArguments && typeArguments.length > index) {
            return typeArguments[index];
        }
        return null;
    }

    private static Type[] getTypeArguments(Type type) {
        if (null == type) {
            return null;
        }

        final ParameterizedType parameterizedType = toParameterizedType(type);
        return (null == parameterizedType) ? null : parameterizedType.getActualTypeArguments();
    }

    private static ParameterizedType toParameterizedType(Type type) {
        ParameterizedType result = null;
        if (type instanceof ParameterizedType) {
            result = (ParameterizedType) type;
        } else if (type instanceof Class) {
            final Class<?> clazz = (Class<?>) type;
            Type genericSuper = clazz.getGenericSuperclass();
            if (null == genericSuper || Object.class.equals(genericSuper)) {
                final Type[] genericInterfaces = clazz.getGenericInterfaces();
                if (genericInterfaces.length > 0) {
                    genericSuper = genericInterfaces[0];
                }
            }
            result = toParameterizedType(genericSuper);
        }
        return result;
    }

}
