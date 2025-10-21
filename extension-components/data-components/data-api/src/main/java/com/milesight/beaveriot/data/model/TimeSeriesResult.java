package com.milesight.beaveriot.data.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * TimeSeriesResult class.
 *
 * @author simon
 * @date 2025/10/13
 */
@Data
public class TimeSeriesResult<T> {
    private List<T> content;

    public static <T> TimeSeriesResult<T> of() {
        return of(new ArrayList<>());
    }

    public static <T> TimeSeriesResult<T> of(List<T> content) {
        TimeSeriesResult<T> result = new TimeSeriesResult<>();
        result.setContent(content);
        return result;
    }
}
