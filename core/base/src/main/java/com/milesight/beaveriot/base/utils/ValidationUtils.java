package com.milesight.beaveriot.base.utils;

/**
 * author: Luxb
 * create: 2025/7/14 8:53
 **/
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class ValidationUtils {
    private static final String REGEX_HEX = "^[0-9a-fA-F]*$";
    private static final String REGEX_URL = "^https?:\\/\\/(?:(?:(?:[a-zA-Z0-9._%+-]+)(?::(?:[a-zA-Z0-9._%+-]+))?@)?(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}|(?:(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d))|\\[(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))\\])(?::(6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}))?(?:\\/(?:[\\p{L}\\p{N}\\-._~!$&'()*+,;=:@\\/]|%[0-9a-fA-F]{2})*)*(?:\\?([a-zA-Z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-fA-F]{2})*)?(?:#(?:[\\p{L}\\p{N}\\-._~!$&'()*+,;=:@\\/?]|%[0-9a-fA-F]{2})*)?$";
    private static final String REGEX_IMAGE_BASE64 = "^data:image/(?:png|jpe?g|gif|webp);base64,(?:[A-Za-z0-9+/]{4}){1,}(?:[A-Za-z0-9+/]{2}(?:==)?|[A-Za-z0-9+/]{3}=?)?$";

    public static boolean isHex(String text) {
        return matches(text, REGEX_HEX);
    }

    public static boolean isURL(String text) {
        return matches(text, REGEX_URL);
    }

    public static boolean isImageBase64(String text) {
        return matches(text, REGEX_IMAGE_BASE64);
    }

    public static boolean isNumber(String text) {
        return NumberUtils.parseDouble(text) != null;
    }

    public static boolean isInteger(String text) {
        return NumberUtils.parseLong(text) != null;
    }

    public static boolean isPositiveInteger(String text) {
        Long value = NumberUtils.parseLong(text);
        return value != null && value > 0;
    }

    public static boolean isNonNegativeInteger(String text) {
        Long value = NumberUtils.parseLong(text);
        return value != null && value >= 0;
    }

    public static boolean matches(String text, String regex) {
        if (text == null || regex == null) {
            return false;
        }
        return text.matches(regex);
    }
}
