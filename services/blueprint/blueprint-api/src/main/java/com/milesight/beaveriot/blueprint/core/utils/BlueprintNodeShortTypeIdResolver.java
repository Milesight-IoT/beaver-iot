package com.milesight.beaveriot.blueprint.core.utils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom TypeIdResolver that shortens fully-qualified class names by replacing
 * a fixed package prefix with a compact symbol during serialization, and restores
 * it back during deserialization.
 */
public class BlueprintNodeShortTypeIdResolver extends TypeIdResolverBase {

    /**
     * Fixed package prefix to shorten. Must include the trailing dot.
     */
    private static final String PACKAGE_PREFIX = "com.milesight.beaveriot.blueprint.core.chart.node.";

    /**
     * Symbol to use when the prefix is shortened.
     */
    private static final String SHORT_SYMBOL = "~";

    private JavaType baseType;

    private final ConcurrentMap<Class<?>, String> classToIdCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, JavaType> idToTypeCache = new ConcurrentHashMap<>();

    @Override
    public void init(JavaType baseType) {
        this.baseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value == null ? null : value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        if (suggestedType == null) {
            return null;
        }
        return classToIdCache.computeIfAbsent(suggestedType, cls -> {
            String fullName = cls.getName();
            if (fullName.startsWith(PACKAGE_PREFIX)) {
                return SHORT_SYMBOL + fullName.substring(PACKAGE_PREFIX.length());
            }
            return fullName;
        });
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Empty type id for base type: " + baseType);
        }
        final TypeFactory factory = context != null ? context.getTypeFactory() : TypeFactory.defaultInstance();
        return idToTypeCache.computeIfAbsent(id, key -> {
            String className = key.startsWith(SHORT_SYMBOL) ? PACKAGE_PREFIX + key.substring(SHORT_SYMBOL.length()) : key;
            try {
                Class<?> raw = Class.forName(className);
                return factory.constructSpecializedType(baseType, raw);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unknown type id: " + key + " -> " + className, e);
            }
        });
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
