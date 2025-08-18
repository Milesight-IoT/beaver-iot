package com.milesight.beaveriot.context.support;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author leon
 */
public class IdentifierValidator {
    public static final String regex = "^[A-Za-z0-9_@#$\\-/\\[\\]:]+$";
    private static final Pattern pattern = Pattern.compile(regex);

    private IdentifierValidator() {
    }

    public static void validate(String identifier) {
        Assert.notNull(identifier, "identifier must not be null");
        Matcher matcher = pattern.matcher(identifier);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("identifier must be a string that matches the pattern ^[A-Za-z0-9_@#$\\-/[]:]+$ :" + identifier);
        }
    }

    public static void validateNullable(String identifier) {
        if (StringUtils.hasText(identifier)) {
            validate(identifier);
        }
    }
}
