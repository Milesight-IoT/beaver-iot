package com.milesight.beaveriot.data.timeseries.influxdb;

/**
 * author: Luxb
 * create: 2025/11/3 8:56
 **/
public class DynamoDbConstants {
    public static final String CONFIG_PREFIX = "timeseries.dynamodb";
    public static final String ENDPOINT_CONFIG = CONFIG_PREFIX + ".endpoint";
    public static final String PARTITION_KEY = "_partition_key";
    public static final String EXPIRE_TIME_KEY = "_expire_time";
    public static final int SAVE_BATCH_SIZE = 25;
    public static final String PLACEHOLDER_NAME = "#";
    public static final String PLACEHOLDER_VALUE = ":";
    public static final String PARTITION_VALUE_SEPARATOR = "-";
}