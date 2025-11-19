package com.milesight.beaveriot.device.service.sheet;

import lombok.Data;

import java.util.Map;

/**
 * DeviceSheetData class.
 *
 * @author simon
 * @date 2025/6/26
 */
@Data
public class DeviceSheetColumn {
    public static final String COLUMN_TYPE_TEXT = "TEXT";

    public static final String COLUMN_TYPE_ENUM = "ENUM";

    public static final String COLUMN_TYPE_BOOLEAN = "BOOLEAN";

    public static final String COLUMN_TYPE_LONG = "LONG";

    public static final String COLUMN_TYPE_DOUBLE = "DOUBLE";

    private String name;

    private String key;

    private String type;

    private Map<String, String> enums;

    private Integer maxLength;

    private Integer minLength;

    private String lengthRange;

    private Double max;

    private Double min;

    private Boolean isHexString;

    private Boolean required = true;

    private Integer fractionDigits;
}
