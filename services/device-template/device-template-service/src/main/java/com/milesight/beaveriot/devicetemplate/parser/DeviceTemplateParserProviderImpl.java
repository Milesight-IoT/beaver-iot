package com.milesight.beaveriot.devicetemplate.parser;

import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public String defaultContent() {
        return deviceTemplateParser.defaultContent();
    }

    @Override
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String jsonData) {
        return deviceTemplateParser.input(integration, deviceTemplateId, jsonData);
    }

    @Override
    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload) {
        return deviceTemplateParser.output(deviceKey, payload);
    }

    @Override
    public DeviceTemplateModel parse(String deviceTemplateContent) {
        return deviceTemplateParser.parse(deviceTemplateContent);
    }

    @Override
    public Device createDevice(String integration, Long deviceTemplateId, String deviceId, String deviceName) {
        return deviceTemplateParser.createDevice(integration, deviceTemplateId, deviceId, deviceName);
    }
}