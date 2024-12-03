package com.milesight.beaveriot.dashboard.context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author loong
 * @date 2024/11/1 8:57
 */
public class DashboardWebSocketContext {

    private static final Map<String, List<String>> entityKeyMap = new ConcurrentHashMap<>();

    public static void addEntityKeys(String key, List<String> entityKeys) {
        entityKeyMap.put(key, entityKeys);
    }

    public static List<String> getEntityKeys(String key) {
        return entityKeyMap.get(key);
    }

    public static void removeEntityKeys(String key) {
        entityKeyMap.remove(key);
    }

    public static List<String> getKeysByValues(List<String> entityKeys) {
        return entityKeyMap.entrySet().stream().filter(entry -> entry.getValue().stream().anyMatch(entityKeys::contains)).map(Map.Entry::getKey).toList();
    }

}
