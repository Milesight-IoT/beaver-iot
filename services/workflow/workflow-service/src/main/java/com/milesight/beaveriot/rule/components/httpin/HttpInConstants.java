package com.milesight.beaveriot.rule.components.httpin;

/**
 * HttpInConstants class.
 *
 * @author simon
 * @date 2025/4/17
 */
public class HttpInConstants {
    private HttpInConstants() {}

    public static final String URL_PREFIX = "/workflow-http-in";

    public static final String AUTH_HEADER = "Authorization";

    public static final String BASIC_AUTH_PREFIX = "Basic ";

    public static final String OUT_HEADER_NAME = "header";

    public static final String OUT_URL_NAME = "url";

    public static final String OUT_BODY_NAME = "body";

    public static final String OUT_PATH_PARAM_NAME = "pathParam";

    public static final String OUT_PARAM_NAME = "params";

    public static final Long MAX_REQUEST_SIZE = (long) 10 * 1024 * 1024;
}
