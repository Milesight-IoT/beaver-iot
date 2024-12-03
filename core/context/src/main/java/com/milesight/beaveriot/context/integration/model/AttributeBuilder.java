package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.base.utils.EnumUtils;
import org.springframework.util.ObjectUtils;

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
    public static final String ATTRIBUTE_FRACTION_DIGITS = "fraction_digits";

    private Map<String, Object> attributes = new HashMap<>();

    public AttributeBuilder unit(String unit) {
        if (ObjectUtils.isEmpty(unit)) {
            return this;
        }
        attributes.put(ATTRIBUTE_UNIT, unit);
        return this;
    }

    public AttributeBuilder max(Double max) {
        if (max == null || max == Double.NaN) {
            return this;
        }
        attributes.put(ATTRIBUTE_MAX, max);
        return this;
    }

    public AttributeBuilder min(Double min) {
        if (min == null || min == Double.NaN) {
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
        attributes.put(ATTRIBUTE_FORMAT, format);
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

    public Map<String, Object> build() {
        return attributes;
    }
}
