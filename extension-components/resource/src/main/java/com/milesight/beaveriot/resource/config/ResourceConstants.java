package com.milesight.beaveriot.resource.config;

import java.util.concurrent.TimeUnit;

/**
 * ResourceConstants class.
 *
 * @author simon
 * @date 2025/4/2
 */
public class ResourceConstants {
    public static final Integer PUT_RESOURCE_PRE_SIGN_EXPIRY_MINUTES = 15;

    public static final String PUBLIC_PATH_PREFIX = "beaver-iot-public";

    public static final String INVALID_OBJECT_NAME_CHARS = "[<>:\"/\\|?*]";

    public static final Integer MAX_OBJECT_NAME_LENGTH = 32;

    public static final Long MAX_FILE_SIZE = (long) (10 * 1024 * 1024);
}
