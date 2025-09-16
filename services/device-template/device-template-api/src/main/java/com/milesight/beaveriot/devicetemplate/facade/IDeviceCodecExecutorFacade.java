package com.milesight.beaveriot.devicetemplate.facade;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/16 9:02
 **/
public interface IDeviceCodecExecutorFacade {
    JsonNode decode(byte[] data, Map<String, Object> argContext);
    byte[] encode(JsonNode data, Map<String, Object> argContext);
}
