package com.milesight.beaveriot.base.utils;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * author: Luxb
 * create: 2025/9/3 16:42
 **/
public class ObjectUtils {
    public static String md5Sum(Object object) {
        try {
            String json = JsonUtils.toJSON(object);
            return DigestUtils.md5Hex(json);
        } catch (Exception e) {
            return null;
        }
    }
}
