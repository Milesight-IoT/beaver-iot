package com.milesight.beaveriot.context.support;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;


public class KeyValidator {

    private static final Pattern pattern = Pattern.compile("^[A-Za-z0-9_@#$.\\-/\\[\\]]+$");

    private KeyValidator() {
    }

    public static boolean isValid(String key) {
        return StringUtils.hasText(key) && pattern.matcher(key).matches();
    }

    public static void validate(String key) {
        if (!isValid(key)) {
            throw new IllegalArgumentException("key must be a string that matches the pattern /^[A-Za-z0-9_@#$.\\-/[]]+$/ , input key: " + key);
        }
    }

    public static void validateNullable(String key) {
        if (StringUtils.hasText(key)) {
            validate(key);
        }
    }

}
