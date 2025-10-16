package com.milesight.beaveriot.device.service.sheet;

/**
 * DeviceSheetConstants class.
 *
 * @author simon
 * @date 2025/6/26
 */
public class DeviceSheetConstants {
    private DeviceSheetConstants() {}

    public static final String DEVICE_NAME_COL_NAME = "Name";

    public static final String DEVICE_NAME_COL_KEY = "name";

    public static final String DEVICE_GROUP_COL_NAME = "Device Group";

    public static final String DEVICE_GROUP_COL_KEY = "device-group";

    public static final String DEVICE_LOCATION_LONGITUDE_COL_NAME = "Longitude";

    public static final String DEVICE_LOCATION_LONGITUDE_COL_KEY = "location-longitude";

    public static final String DEVICE_LOCATION_LATITUDE_COL_NAME = "Latitude";

    public static final String DEVICE_LOCATION_LATITUDE_COL_KEY = "location-latitude";

    public static final String DEVICE_LOCATION_ADDRESS_COL_NAME = "Address";

    public static final String DEVICE_LOCATION_ADDRESS_COL_KEY = "location-address";

    public static final String DEVICE_SHEET_NAME = "DEVICES_LIST";

    public static final String HIDDEN_OPTION_SHEET = "_HIDDEN_OPT";

    public static final String HIDDEN_COL_META_SHEET = "_HIDDEN_COL_META";

    protected static final String[] REQUIRED_SHEETS = {
            DEVICE_SHEET_NAME,
            HIDDEN_COL_META_SHEET,
    };

    public static final String HIDDEN_COL_META_INDEX = "colIndex";

    public static final String HIDDEN_COL_META_KEY = "key";

    public static final String HIDDEN_COL_META_NAME = "name";

    public static final int MAX_BATCH_NUMBER = 500;

    public static final int ADDITIONAL_COLUMN_WIDTH = 1000;

    public static final short HEADER_ROW_HEIGHT = 500;

    public static final String DEVICE_SHEET_ERROR_COL_NAME = "Error";
}
