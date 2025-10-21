package com.milesight.beaveriot.data.model;

import com.milesight.beaveriot.data.filterable.Filterable;
import lombok.Data;

import java.util.function.Consumer;

/**
 * TimeSeriesBaseQuery class.
 *
 * @author simon
 * @date 2025/10/13
 */
@Data
public class TimeSeriesBaseQuery {
    private Consumer<Filterable> filterable;
}
