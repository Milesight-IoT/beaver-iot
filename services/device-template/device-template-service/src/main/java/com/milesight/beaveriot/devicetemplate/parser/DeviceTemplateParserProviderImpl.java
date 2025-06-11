package com.milesight.beaveriot.devicetemplate.parser;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * author: Luxb
 * create: 2025/5/15 10:22
 **/
@Slf4j
@Service
public class DeviceTemplateParserProviderImpl implements DeviceTemplateParserProvider {
    private final DeviceTemplateParser deviceTemplateParser;

    public DeviceTemplateParserProviderImpl(DeviceTemplateParser deviceTemplateParser) {
        this.deviceTemplateParser = deviceTemplateParser;
    }

    @Override
    public boolean validate(String deviceTemplateContent) {
        return deviceTemplateParser.validate(deviceTemplateContent);
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
        if (!deviceTemplateParser.validate(deviceTemplateContent)) {
            return null;
        }
        return deviceTemplateParser.discover(integration, data, deviceTemplateId, deviceTemplateContent);
    }
}
