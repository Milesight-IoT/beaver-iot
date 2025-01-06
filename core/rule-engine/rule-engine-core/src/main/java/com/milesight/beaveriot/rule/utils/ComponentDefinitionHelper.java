package com.milesight.beaveriot.rule.utils;

import org.springframework.util.ObjectUtils;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author leon
 */
@SuppressWarnings({"java:S1192", "java:S3776"})
public class ComponentDefinitionHelper {

    private ComponentDefinitionHelper() {
    }

    public static Object getDefaultValue(Object defaultValue, String fieldTypeName, boolean isDuration) {
        // special for boolean as it should not be literal
        if ("boolean".equals(fieldTypeName)) {
            if (ObjectUtils.isEmpty(defaultValue)) {
                defaultValue = false;
            } else {
                defaultValue = "true".equalsIgnoreCase(defaultValue.toString());
            }
        }
        if (!isDuration) {
            // special for integer as it should not be literal
            if ("int".equals(fieldTypeName) && (!ObjectUtils.isEmpty(defaultValue) && defaultValue instanceof String)) {
                defaultValue = Integer.parseInt(defaultValue.toString());
            }
            // special for long as it should not be literal
            if ("long".equals(fieldTypeName) && (!ObjectUtils.isEmpty(defaultValue) && defaultValue instanceof String)) {
                defaultValue = Long.parseLong(defaultValue.toString());
            }
            // special for double as it should not be literal
            if ("double".equals(fieldTypeName) && (!ObjectUtils.isEmpty(defaultValue) && defaultValue instanceof String)) {
                defaultValue = Double.parseDouble(defaultValue.toString());
            }
            // special for double as it should not be literal
            if ("float".equals(fieldTypeName) && (!ObjectUtils.isEmpty(defaultValue) && defaultValue instanceof String)) {
                defaultValue = Float.parseFloat(defaultValue.toString());
            }
        }
        if (ObjectUtils.isEmpty(defaultValue)) {
            defaultValue = "";
        }
        return defaultValue;
    }

    public static List<String> gatherEnums(String enums, Class<?> fieldTypeElement) {
        if (!ObjectUtils.isEmpty(enums)) {
            String[] values = enums.split(",");
            return Stream.of(values).map(String::trim).toList();
        } else if (fieldTypeElement.isEnum()) {
            return doGatherFromEnum(fieldTypeElement);
        }

        return Collections.emptyList();
    }

    private static List<String> doGatherFromEnum(Class<?> fieldTypeElement) {
        final List<String> enums = new ArrayList<>();

        for (Object val : fieldTypeElement.getEnumConstants()) {
            String str = val.toString();
            if (!enums.contains(str)) {
                enums.add(str);
            }
        }

        return enums;
    }

    /**
     * Gets the JSON schema type.
     *
     * @param type the java type
     * @return the json schema type, is never null, but returns <tt>object</tt> as the generic type
     */
    public static String getType(String type, boolean enumType, boolean isDuration, boolean isMap) {
        if (isMap) {
            return "map";
        } else if (enumType) {
            return "enum";
        } else if (isDuration) {
            return "duration";
        } else if (type == null) {
            // return generic type for unknown type
            return "object";
        } else if (type.equals(URI.class.getName()) || type.equals(URL.class.getName())) {
            return "string";
        } else if (type.equals(File.class.getName())) {
            return "string";
        } else if (type.equals(Date.class.getName())) {
            return "string";
        } else if (type.startsWith("java.lang.Class")) {
            return "string";
        } else if (type.startsWith("java.util.List") || type.startsWith("java.util.Collection")) {
            return "array";
        }

        String primitive = getPrimitiveType(type);
        if (primitive != null) {
            return primitive;
        }

        return "object";
    }

    /**
     * Gets the JSON schema primitive type.
     *
     * @param name the java type
     * @return the json schema primitive type, or <tt>null</tt> if not a primitive
     */
    public static String getPrimitiveType(String name) {
        // special for byte[] or Object[] as its common to use
        if ("java.lang.byte[]".equals(name) || "byte[]".equals(name)) {
            return "string";
        } else if ("java.lang.Byte[]".equals(name) || "Byte[]".equals(name)) {
            return "array";
        } else if ("java.lang.Object[]".equals(name) || "Object[]".equals(name)) {
            return "array";
        } else if ("java.lang.String[]".equals(name) || "String[]".equals(name)) {
            return "array";
        } else if ("java.lang.Character".equals(name) || "Character".equals(name) || "char".equals(name)) {
            return "string";
        } else if ("java.lang.String".equals(name) || "String".equals(name)) {
            return "string";
        } else if ("java.lang.Boolean".equals(name) || "Boolean".equals(name) || "boolean".equals(name)) {
            return "boolean";
        } else if ("java.lang.Integer".equals(name) || "Integer".equals(name) || "int".equals(name)) {
            return "integer";
        } else if ("java.lang.Long".equals(name) || "Long".equals(name) || "long".equals(name)) {
            return "integer";
        } else if ("java.lang.Short".equals(name) || "Short".equals(name) || "short".equals(name)) {
            return "integer";
        } else if ("java.lang.Byte".equals(name) || "Byte".equals(name) || "byte".equals(name)) {
            return "integer";
        } else if ("java.lang.Float".equals(name) || "Float".equals(name) || "float".equals(name)) {
            return "number";
        } else if ("java.lang.Double".equals(name) || "Double".equals(name) || "double".equals(name)) {
            return "number";
        }

        return null;
    }

}
