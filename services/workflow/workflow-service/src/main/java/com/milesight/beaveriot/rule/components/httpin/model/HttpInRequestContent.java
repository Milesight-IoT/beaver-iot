package com.milesight.beaveriot.rule.components.httpin.model;

import lombok.Data;

import java.util.Map;

/**
 * HttpInRequestContent class.
 *
 * @author simon
 * @date 2025/4/17
 */
@Data
public class HttpInRequestContent {
    private String method;

    private Map<String, String> headers;

    private String url;

    private Map<String, String> pathParams;

    private Map<String, String> params;

    private String body;
}
