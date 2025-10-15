package com.milesight.beaveriot.devicetemplate.codec.service;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.facade.IBlueprintLibraryResourceResolverFacade;
import com.milesight.beaveriot.blueprint.model.BlueprintDeviceCodec;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.devicetemplate.codec.Argument;
import com.milesight.beaveriot.devicetemplate.codec.CodecExecutor;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorChain;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorDecoderChain;
import com.milesight.beaveriot.devicetemplate.codec.chain.CodecExecutorEncoderChain;
import com.milesight.beaveriot.devicetemplate.codec.enums.CodecErrorCode;
import com.milesight.beaveriot.devicetemplate.facade.ICodecExecutorFacade;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceCodecExecutorFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * author: Luxb
 * create: 2025/9/8 16:43
 **/
@Service
public class CodecExecutorService implements ICodecExecutorFacade {
    private final IBlueprintLibraryResourceResolverFacade blueprintLibraryResourceResolverFacade;
    private final DeviceTemplateParserProvider deviceTemplateParserProvider;

    public CodecExecutorService(IBlueprintLibraryResourceResolverFacade blueprintLibraryResourceResolverFacade, DeviceTemplateParserProvider deviceTemplateParserProvider) {
        this.blueprintLibraryResourceResolverFacade = blueprintLibraryResourceResolverFacade;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
    }

    @Override
    public IDeviceCodecExecutorFacade getDeviceCodecExecutor(BlueprintLibrary blueprintLibrary, String vendor, String model) {
        String deviceTemplateContent = blueprintLibraryResourceResolverFacade.getDeviceTemplateContent(blueprintLibrary, vendor, model);
        if (deviceTemplateContent == null) {
            throw ServiceException.with(CodecErrorCode.CODEC_EXECUTOR_BUILD_FAILED).build();
        }

        DeviceTemplateModel deviceTemplateModel = deviceTemplateParserProvider.parse(deviceTemplateContent);
        if (deviceTemplateModel == null) {
            throw ServiceException.with(CodecErrorCode.CODEC_EXECUTOR_BUILD_FAILED).build();
        }

        DeviceTemplateModel.Codec codec = deviceTemplateModel.getCodec();
        if (codec == null) {
            return null;
        }

        BlueprintDeviceCodec blueprintDeviceCodec = blueprintLibraryResourceResolverFacade.getBlueprintDeviceCodec(blueprintLibrary, vendor, codec.getRef(), codec.getId());
        if (blueprintDeviceCodec == null) {
            throw ServiceException.with(CodecErrorCode.CODEC_EXECUTOR_BUILD_FAILED).build();
        }

        if (!blueprintDeviceCodec.validate()) {
            throw ServiceException.with(CodecErrorCode.CODEC_EXECUTOR_BUILD_FAILED).build();
        }

        CodecExecutorDecoderChain decoderChain = createCodecExecutorChain(blueprintLibrary,
                vendor,
                () -> CodecExecutorDecoderChain.builder().build(),
                blueprintDeviceCodec.getDecoder().getChain());
        if (decoderChain == null) {
            throw ServiceException.with(CodecErrorCode.CODEC_EXECUTOR_BUILD_FAILED).build();
        }

        CodecExecutorEncoderChain encoderChain = createCodecExecutorChain(blueprintLibrary,
                vendor,
                () -> CodecExecutorEncoderChain.builder().build(),
                blueprintDeviceCodec.getEncoder().getChain());
        if (encoderChain == null) {
            throw ServiceException.with(CodecErrorCode.CODEC_EXECUTOR_BUILD_FAILED).build();
        }

        return DeviceCodecExecutorService.of(decoderChain, encoderChain);
    }

    private <T extends CodecExecutorChain> T createCodecExecutorChain(BlueprintLibrary blueprintLibrary, String vendor, Supplier<T> chainBuilder, List<BlueprintDeviceCodec.Codec> chain) {
        T codecExecutorChain = chainBuilder.get();
        for (BlueprintDeviceCodec.Codec codec : chain) {
            String code = blueprintLibraryResourceResolverFacade.getResourceContent(blueprintLibrary, vendor, codec.getScript());
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
