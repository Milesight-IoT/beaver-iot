package com.milesight.beaveriot.data.api;


import com.milesight.beaveriot.data.support.TimeSeriesDataConverter;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
public @interface SupportTimeSeries {
    String category();

    String tableName() default "";

    Class<? extends TimeSeriesDataConverter> converter() default TimeSeriesDataConverter.class;

    String timeColumn();

    String[] indexedColumns() default {};
}
