package com.milesight.beaveriot.base.utils;

import com.milesight.beaveriot.base.enums.EnumCode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author leon
 */
public class EnumUtils {

    private EnumUtils() {
    }

    public static <E extends Enum> Map<String, String> getEnumMap(Class<E> enumClass) {
        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (final Enum e : enumClass.getEnumConstants()) {
            if (e instanceof EnumCode enumCode) {
                map.put(enumCode.getCode(), enumCode.getValue());
            } else {
                map.put(String.valueOf(e.ordinal()), e.name());
            }
        }
        return map;
    }

}
