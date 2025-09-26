package com.milesight.beaveriot.devicetemplate.parser;

import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.integration.model.BlueprintCreationStrategy;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.BiFunction;

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
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, Object data) {
        return deviceTemplateParser.input(integration, deviceTemplateId, data);
    }

    @Override
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String deviceIdentifier, String deviceName, Object data) {
        return deviceTemplateParser.input(integration, deviceTemplateId, deviceIdentifier, deviceName, data);
    }

    @Override
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, Object data, Map<String, Object> codecArgContext) {
        return deviceTemplateParser.input(integration, deviceTemplateId, data, codecArgContext);
    }

    @Override
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String deviceIdentifier, String deviceName, Object data, Map<String, Object> codecArgContext) {
        return deviceTemplateParser.input(integration, deviceTemplateId, deviceIdentifier, deviceName, data, codecArgContext);
    }

    @Override
    public DeviceTemplateInputResult input(String deviceKey, Object data, Map<String, Object> codecArgContext) {
        return deviceTemplateParser.input(deviceKey, data, codecArgContext);
    }

    @Override
    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload) {
        return deviceTemplateParser.output(deviceKey, payload);
    }

    @Override
    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload, Map<String, Object> codecArgContext) {
        return deviceTemplateParser.output(deviceKey, payload, codecArgContext);
    }

    @Override
    public DeviceTemplateModel parse(String deviceTemplateContent) {
        return deviceTemplateParser.parse(deviceTemplateContent);
    }

    @Override
    public Device createDevice(String integration, Long deviceTemplateId, String deviceIdentifier, String deviceName) {
        return deviceTemplateParser.createDevice(integration, deviceTemplateId, deviceIdentifier, deviceName);
    }

    @Override
    public Device createDevice(String integration,
                              String vendor,
                              String model,
                              String deviceIdentifier,
                              String deviceName,
                              BiFunction<Device, Map<String, Object>, Boolean> beforeSaveDevice,
                              BlueprintCreationStrategy strategy) {
        return deviceTemplateParser.createDevice(integration,
                vendor,
                model,
                deviceIdentifier,
                deviceName,
                beforeSaveDevice,
                strategy);
    }

    @Override
    public Device createDevice(String integration,
                               String vendor,
                               String model,
                               String deviceIdentifier,
                               String deviceName,
                               BiFunction<Device, Map<String, Object>, Boolean> beforeSaveDevice) {
        return deviceTemplateParser.createDevice(integration,
                vendor,
                model,
                deviceIdentifier,
                deviceName,
                beforeSaveDevice);
    }

    @Override
    public DeviceTemplate getLatestDeviceTemplate(String vendor, String model) {
        return deviceTemplateParser.getLatestDeviceTemplate(vendor, model);
    }
}