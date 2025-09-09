package com.milesight.beaveriot.devicetemplate.codec.service;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.component.BlueprintRepositoryResourceResolver;
import com.milesight.beaveriot.blueprint.model.BlueprintDeviceCodec;
import com.milesight.beaveriot.context.api.CodecExecutorServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.devicetemplate.codec.Argument;
import com.milesight.beaveriot.devicetemplate.codec.CodecExecutor;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorChain;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorDecoderChain;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorEncoderChain;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * author: Luxb
 * create: 2025/9/8 16:43
 **/
@Service
public class CodecExecutorService implements CodecExecutorServiceProvider {
    private final BlueprintRepositoryResourceResolver blueprintRepositoryResourceResolver;
    private final DeviceTemplateParserProvider deviceTemplateParserProvider;

    public CodecExecutorService(BlueprintRepositoryResourceResolver blueprintRepositoryResourceResolver, DeviceTemplateParserProvider deviceTemplateParserProvider) {
        this.blueprintRepositoryResourceResolver = blueprintRepositoryResourceResolver;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
    }

    @Override
    public DeviceCodecExecutorService getDeviceCodecExecutor(String vendor, String model) {
        String deviceTemplateContent = blueprintRepositoryResourceResolver.getDeviceTemplateContent(vendor, model);
        if (deviceTemplateContent == null) {
            return null;
        }

        DeviceTemplateModel deviceTemplateModel = deviceTemplateParserProvider.parse(deviceTemplateContent);
        if (deviceTemplateModel == null) {
            return null;
        }

        DeviceTemplateModel.Codec codec = deviceTemplateModel.getCodec();
        if (codec == null) {
            return null;
        }

        BlueprintDeviceCodec blueprintDeviceCodec = blueprintRepositoryResourceResolver.getBlueprintDeviceCodec(vendor, codec.getRef(), codec.getId());
        if (blueprintDeviceCodec == null) {
            return null;
        }

        if (!blueprintDeviceCodec.validate()) {
            return null;
        }

        CodecExecutorDecoderChain decoderChain = createCodecExecutorChain(vendor,
                () -> CodecExecutorDecoderChain.builder().build(),
                blueprintDeviceCodec.getDecoder().getChain());
        if (decoderChain == null) {
            return null;
        }

        CodecExecutorEncoderChain encoderChain = createCodecExecutorChain(vendor,
                () -> CodecExecutorEncoderChain.builder().build(),
                blueprintDeviceCodec.getEncoder().getChain());
        if (encoderChain == null) {
            return null;
        }

        return DeviceCodecExecutorService.of(decoderChain, encoderChain);
    }

    private <T extends CodecExecutorChain> T createCodecExecutorChain(String vendor, Supplier<T> chainBuilder, List<BlueprintDeviceCodec.Codec> chain) {
        T codecExecutorChain = chainBuilder.get();
        for (BlueprintDeviceCodec.Codec codec : chain) {
            String code = blueprintRepositoryResourceResolver.getResourceContent(vendor, codec.getScript());
            if (StringUtils.isEmpty(code)) {
                return null;
            }

            codecExecutorChain.addExecutor(CodecExecutor.builder()
                    .code(code)
                    .entry(codec.getEntry())
                    .arguments(convertArgument(codec.getArgs()))
                    .build());
        }

        return codecExecutorChain;
    }

    private List<Argument> convertArgument(List<BlueprintDeviceCodec.Argument> arguments) {
        return arguments.stream().map(argument -> Argument.of(argument.getId(), argument.isPayload())).toList();
    }
}
