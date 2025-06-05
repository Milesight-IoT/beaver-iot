package com.milesight.beaveriot.resource.config;

/**
 * ResourceConstants class.
 *
 * @author simon
 * @date 2025/4/2
 */
public class ResourceConstants {
    private ResourceConstants() {}

    public static final String PUBLIC_PATH_PREFIX = "beaver-iot-public";

    public static final String INVALID_OBJECT_NAME_CHARS = "[<>:\"/\\|?*]";

    public static final Integer MAX_OBJECT_NAME_LENGTH = 255;

    public static final Long MAX_FILE_SIZE = (long) (10 * 1024 * 1024);
}
