package com.milesight.beaveriot.devicetemplate.codec.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.devicetemplate.codec.CodecExecutor;
import com.milesight.beaveriot.devicetemplate.codec.ReturnType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/8 13:42
 **/
@Builder
@EqualsAndHashCode(callSuper = true)
@Data
public class CodecExecutorDecoderChain extends CodecExecutorChain {
    public JsonNode execute(Object data, Map<String, Object> argContext) {
        return (JsonNode) super.execute(data, argContext);
    }

    @Override
    protected void initLastExecutor(CodecExecutor executor) {
        executor.setReturnType(ReturnType.JSON);
    }
}