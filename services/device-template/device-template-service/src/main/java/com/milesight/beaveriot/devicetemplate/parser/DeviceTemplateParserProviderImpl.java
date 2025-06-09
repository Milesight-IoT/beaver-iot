package com.milesight.beaveriot.devicetemplate.parser;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.model.DeviceTemplateType;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/15 10:22
 **/
@Slf4j
@Service
public class DeviceTemplateParserProviderImpl implements DeviceTemplateParserProvider {
    private final CommonDeviceTemplateParser commonDeviceTemplateParser;
    private final Map<String, DeviceTemplateParser> registerParserMap;

    public DeviceTemplateParserProviderImpl(CommonDeviceTemplateParser commonDeviceTemplateParser, JsonDeviceTemplateParser jsonDeviceTemplateParser) {
        this.commonDeviceTemplateParser = commonDeviceTemplateParser;
        registerParserMap = Map.of(
                DeviceTemplateType.JSON.toString(), jsonDeviceTemplateParser
        );
    }

    @Override
    public boolean validate(String deviceTemplateContent) {
        String templateType = commonDeviceTemplateParser.getTemplateType(deviceTemplateContent);
        DeviceTemplateParser parser = registerParserMap.get(templateType);
        if (parser == null) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), MessageFormat.format("template type:{0} parser not found", templateType)).build();
        }
        return parser.validate(deviceTemplateContent);
    }

    @Override
    public String getDefaultDeviceTemplateContent() {
        try {
            return StreamUtils.copyToString(new ClassPathResource("template/default_device_template.yaml").getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    @Override
    public DeviceTemplateDiscoverResponse discover(String integration, Object data, Long deviceTemplateId, String deviceTemplateContent) {
        String templateType = commonDeviceTemplateParser.getTemplateType(deviceTemplateContent);
        DeviceTemplateParser parser = registerParserMap.get(templateType);
        if (!parser.validate(deviceTemplateContent)) {
            return null;
        }
        return parser.discover(integration, data, deviceTemplateId, deviceTemplateContent);
    }
}
