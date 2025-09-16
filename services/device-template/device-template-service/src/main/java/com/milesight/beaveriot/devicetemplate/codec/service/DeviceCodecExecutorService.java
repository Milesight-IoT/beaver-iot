package com.milesight.beaveriot.devicetemplate.codec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorDecoderChain;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorEncoderChain;
import com.milesight.beaveriot.devicetemplate.codec.enums.CodecErrorCode;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceCodecExecutorFacade;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/8 17:00
 **/
@Slf4j
public class DeviceCodecExecutorService implements IDeviceCodecExecutorFacade {
    private CodecExecutorDecoderChain decoderChain;
    private CodecExecutorEncoderChain encoderChain;

    public static DeviceCodecExecutorService of(CodecExecutorDecoderChain decoderChain, CodecExecutorEncoderChain encoderChain) {
        DeviceCodecExecutorService executor = new DeviceCodecExecutorService();
        executor.decoderChain = decoderChain;
        executor.encoderChain = encoderChain;
        return executor;
    }

    @Override
    public JsonNode decode(byte[] data, Map<String, Object> argContext) {
        try {
            return decoderChain.execute(data, argContext);
        } catch (Exception e) {
            log.error("DeviceCodecExecutor decode error", e);
            throw ServiceException.with(CodecErrorCode.CODEC_DECODE_FAILED.getErrorCode(), CodecErrorCode.CODEC_DECODE_FAILED.getErrorMessage())
                    .detailMessage(e.getMessage()).build();
        }
    }

    @Override
    public byte[] encode(JsonNode data, Map<String, Object> argContext) {
        try {
            return encoderChain.execute(data, argContext);
        } catch (Exception e) {
            log.error("DeviceCodecExecutor encode error", e);
            throw ServiceException.with(CodecErrorCode.CODEC_ENCODE_FAILED.getErrorCode(), CodecErrorCode.CODEC_ENCODE_FAILED.getErrorMessage())
                    .detailMessage(e.getMessage()).build();
        }
    }
}
