package com.milesight.beaveriot.data.support;

import com.milesight.beaveriot.base.utils.JsonUtils;

import java.util.Map;

public class TimeSeriesDataConverter {
    public Map<String, Object> toMap(Object po) {
        return JsonUtils.toMap(po);
    }

    public <T> T fromMap(Map<String, Object> map, Class<T> classType) {
        return JsonUtils.cast(map, classType);
    }
}
