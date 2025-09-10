package com.milesight.beaveriot.context.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/9 9:13
 **/
public interface DeviceCodecExecutorProvider {
    JsonNode decode(byte[] data, Map<String, Object> argContext);
    byte[] encode(JsonNode data, Map<String, Object> argContext);
}
