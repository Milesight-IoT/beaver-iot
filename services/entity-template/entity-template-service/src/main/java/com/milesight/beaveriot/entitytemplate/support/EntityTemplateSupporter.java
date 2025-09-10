package com.milesight.beaveriot.entitytemplate.support;

/**
 * author: Luxb
 * create: 2025/8/21 8:53
 **/
public class EntityTemplateSupporter {
    public static String getIdentifierFromKey(String key) {
        if (key == null) {
            return null;
        }

        if (!key.contains(".")) {
            return key;
        }

        return key.substring(key.lastIndexOf(".") + 1);
    }
}
