package com.milesight.beaveriot.base.utils;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

/**
 * author: Luxb
 * create: 2025/7/30 10:50
 **/
public class NumberUtils {
    public static Double parseDouble(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj instanceof Number number ? Double.valueOf(number.doubleValue()) : Doubles.tryParse(obj.toString());
    }

    public static Long parseLong(Object obj) {
        if (obj == null) {
            return null;
        }
        return Longs.tryParse(obj.toString());
    }
}
