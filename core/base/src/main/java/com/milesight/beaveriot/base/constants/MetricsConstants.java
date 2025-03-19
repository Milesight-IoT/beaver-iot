package com.milesight.beaveriot.base.constants;

/**
 * @author leon
 */
public class MetricsConstants {


    private MetricsConstants() {
    }

    /**
     * metrics name for Exchange repeat execution max times in the route
     */
    public static final String METRICS_EXCHANGE_EXECUTION_REPEAT_MAX = "camel.exchanges.execution.repeat.max";

    /**
     * metrics default none value
     */
    public static final String DEFAULT_NONE_VALUE = "none";

    /**
     * metrics tag for Exchange route name
     */
    public static final String ROUTE_NAME_TAG = "routeName";

    public static final String ROUTE_ROUTE_ID = "routeId";

    public static final String ROUTE_ROOT_ROUTE_ID = "rootRouteId";
}
