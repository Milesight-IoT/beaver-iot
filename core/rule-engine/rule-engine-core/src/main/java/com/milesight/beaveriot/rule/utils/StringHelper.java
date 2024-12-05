package com.milesight.beaveriot.rule.utils;

/**
 * @author leon
 */
public class StringHelper {

    private StringHelper() {
    }

    public static String upperFirst(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static String lowerFirst(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public static String after(String text, String after) {
        int index = text.indexOf(after);
        return index < 0 ? null : text.substring(index + after.length());
    }

    public static String before(String text, String before) {
        int index = text.indexOf(before);
        return index < 0 ? null : text.substring(0, index);
    }

}
