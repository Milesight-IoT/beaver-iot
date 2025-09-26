package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.base.utils.EnumUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author leon
 */
public class AttributeBuilder {

    public static final int POSITIVE_INT_NAN = -1;
    public static final String ATTRIBUTE_UNIT = "unit";
    public static final String ATTRIBUTE_MAX = "max";
    public static final String ATTRIBUTE_MIN = "min";
    public static final String ATTRIBUTE_MAX_LENGTH = "max_length";
    public static final String ATTRIBUTE_MIN_LENGTH = "min_length";
    public static final String ATTRIBUTE_ENUM = "enum";
    public static final String ATTRIBUTE_FORMAT = "format";
    public static final String ATTRIBUTE_FORMAT_VALUE_HEX = "HEX";
    public static final String ATTRIBUTE_FORMAT_VALUE_IMAGE = "IMAGE";
    public static final String ATTRIBUTE_FORMAT_VALUE_IMAGE_URL = "IMAGE:URL";
    public static final String ATTRIBUTE_FORMAT_VALUE_IMAGE_BASE64 = "IMAGE:BASE64";
    public static final String ATTRIBUTE_FORMAT_VALUE_REGEX = "REGEX";
    public static final String ATTRIBUTE_FRACTION_DIGITS = "fraction_digits";
    public static final String ATTRIBUTE_OPTIONAL = "optional";
    public static final String ATTRIBUTE_LENGTH_RANGE = "length_range";
    public static final String ATTRIBUTE_DEFAULT_VALUE = "default_value";
    public static final String ATTRIBUTE_IMPORTANT = "important";

    private final Map<String, Object> attributes = new HashMap<>();

    public AttributeBuilder unit(String unit) {
        if (ObjectUtils.isEmpty(unit)) {
            return this;
        }
        attributes.put(ATTRIBUTE_UNIT, unit);
        return this;
    }

    public AttributeBuilder max(Double max) {
        if (max == null || Double.isNaN(max)) {
            return this;
        }
        attributes.put(ATTRIBUTE_MAX, max);
        return this;
    }

    public AttributeBuilder min(Double min) {
        if (min == null || Double.isNaN(min)) {
            return this;
        }
        attributes.put(ATTRIBUTE_MIN, min);
        return this;
    }

    public AttributeBuilder maxLength(Integer maxLength) {
        if (maxLength == null || maxLength == POSITIVE_INT_NAN) {
            return this;
        }
        attributes.put(ATTRIBUTE_MAX_LENGTH, maxLength);
        return this;
    }

    public AttributeBuilder minLength(Integer minLength) {
        if (minLength == null || minLength == POSITIVE_INT_NAN) {
            return this;
        }
        attributes.put(ATTRIBUTE_MIN_LENGTH, minLength);
        return this;
    }

    public AttributeBuilder format(String format) {
        if (ObjectUtils.isEmpty(format)) {
            return this;
        }

        String[] parts = format.split(":", 2);
        AttributeFormatComponent component = AttributeFormatComponent.valueOf(parts[0]);
        if (parts.length == 1 || !StringUtils.hasText(parts[1])) {
            return this.format(component, null);
        }

        return this.format(component, parts[1]);
    }

    public AttributeBuilder format(AttributeFormatComponent component, String args) {
        if (component == null) {
            return this;
        }

        if (StringUtils.hasText(args)) {
            attributes.put(ATTRIBUTE_FORMAT, component + ":" + args);
        } else {
            attributes.put(ATTRIBUTE_FORMAT, component.toString());
        }

        return this;
    }

    public AttributeBuilder defaultValue(String v) {
        if (ObjectUtils.isEmpty(v)) {
            return this;
        }

        attributes.put(ATTRIBUTE_DEFAULT_VALUE, v);

        return this;
    }

    public AttributeBuilder lengthRange(String v) {
        if (ObjectUtils.isEmpty(v)) {
            return this;
        }

        attributes.put(ATTRIBUTE_LENGTH_RANGE, v);

        return this;
    }

    public AttributeBuilder fractionDigits(Integer fractionDigits) {
        if (fractionDigits == null || fractionDigits == POSITIVE_INT_NAN) {
            return this;
        }
        attributes.put(ATTRIBUTE_FRACTION_DIGITS, fractionDigits);
        return this;
    }

    public AttributeBuilder enums(Class<? extends Enum> enumClass) {
        if (enumClass == null) {
            return this;
        }
        attributes.put(ATTRIBUTE_ENUM, EnumUtils.getEnumMap(enumClass));
        return this;
    }

    public AttributeBuilder enums(Map<String, String> enums) {
        if (ObjectUtils.isEmpty(enums)) {
            return this;
        }
        attributes.put(ATTRIBUTE_ENUM, enums);
        return this;
    }

    public AttributeBuilder optional(boolean optional) {
        if (!optional) {
            return this;
        }

        attributes.put(ATTRIBUTE_OPTIONAL, optional);
        return this;
    }

    public AttributeBuilder important(Integer important) {
        if (important == null || important == POSITIVE_INT_NAN) {
            return this;
        }

        attributes.put(ATTRIBUTE_IMPORTANT, important);
        return this;
    }

    public Map<String, Object> build() {
        return attributes;
    }
}
