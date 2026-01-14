package com.milesight.beaveriot.base.utils;

import java.util.Map;

/**
 * @author Luxb
 * @date 2026/1/7 15:33
 **/
public class MapUtils {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(Map<String, Object> map) {
        String json = JsonUtils.withDefaultStrategy().toJSON(map);
        return JsonUtils.withDefaultStrategy().fromJSON(json, Map.class);
    }
}
