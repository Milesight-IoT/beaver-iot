package com.milesight.beaveriot.base.utils.snowflake;

/**
 * @author leon
 */
public class SnowflakeUtil {

    private SnowflakeUtil() {
    }

    private static final Sequence snowflake = new Sequence();

    public static Sequence getSnowflake() {
        return snowflake;
    }

    public static long nextId() {
        return snowflake.nextId();
    }
}
