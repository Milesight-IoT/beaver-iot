package com.milesight.beaveriot.metrics.utils;

import io.micrometer.core.instrument.Tags;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class TagUtils {

    private TagUtils() {
    }

    public static Map<String,String> toMap(Tags tags) {
        return tags.stream().map(tag -> Map.entry(tag.getKey(), tag.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
