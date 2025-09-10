package com.milesight.beaveriot.devicetemplate.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.blueprint.library.support.DefaultTemplateLoader;
import com.milesight.beaveriot.context.api.*;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import com.milesight.beaveriot.context.i18n.message.MergedResourceBundleMessageSource;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;
import com.milesight.beaveriot.context.support.TemplateLoader;
import com.milesight.beaveriot.devicetemplate.enums.ServerErrorCode;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateParserFacade;
import com.milesight.beaveriot.devicetemplate.support.DeviceTemplateHelper;
import com.networknt.schema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;

/**
 * author: Luxb
 * create: 2025/5/15 13:22
 **/
@Slf4j
@Service
public class DeviceTemplateParser implements IDeviceTemplateParserFacade {
    private final IntegrationServiceProvider integrationServiceProvider;
    private final DeviceServiceProvider deviceServiceProvider;
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    private final BlueprintLibraryResourceProvider blueprintLibraryResourceProvider;
    private final DeviceBlueprintMappingServiceProvider deviceBlueprintMappingServiceProvider;
    private final BlueprintServiceProvider blueprintServiceProvider;
    private final EntityServiceProvider entityServiceProvider;
    private final CodecExecutorServiceProvider codecExecutorServiceProvider;
    private final MergedResourceBundleMessageSource messageSource;

    public DeviceTemplateParser(IntegrationServiceProvider integrationServiceProvider,
                                DeviceServiceProvider deviceServiceProvider,
                                DeviceTemplateServiceProvider deviceTemplateServiceProvider,
                                BlueprintLibraryResourceProvider blueprintLibraryResourceProvider,
                                DeviceBlueprintMappingServiceProvider deviceBlueprintMappingServiceProvider,
                                BlueprintServiceProvider blueprintServiceProvider,
                                EntityServiceProvider entityServiceProvider,
                                @Lazy CodecExecutorServiceProvider codecExecutorServiceProvider,
                                MergedResourceBundleMessageSource messageSource) {
        this.integrationServiceProvider = integrationServiceProvider;
        this.deviceServiceProvider = deviceServiceProvider;
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.blueprintLibraryResourceProvider = blueprintLibraryResourceProvider;
        this.deviceBlueprintMappingServiceProvider = deviceBlueprintMappingServiceProvider;
        this.blueprintServiceProvider = blueprintServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.codecExecutorServiceProvider = codecExecutorServiceProvider;
        this.messageSource = messageSource;
    }

    @Override
    public String defaultContent() {
        try {
            return StreamUtils.copyToString(new ClassPathResource("template/default_device_template.yaml").getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    public boolean validate(String deviceTemplateContent) {
        try {
            if (StringUtils.isEmpty(deviceTemplateContent)) {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_EMPTY.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_EMPTY.getErrorMessage()).build();
            }

            Yaml yaml = new Yaml();
            Object loadedYaml = yaml.load(deviceTemplateContent);
            if (loadedYaml == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_EMPTY.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_EMPTY.getErrorMessage()).build();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonData = mapper.readTree(mapper.writeValueAsBytes(loadedYaml));

            InputStream schemaInputStream = DeviceTemplateParser.class.getClassLoader()
                    .getResourceAsStream("template/device_template_schema.json");
            if (schemaInputStream == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_SCHEMA_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_SCHEMA_NOT_FOUND.getErrorMessage()).build();
            }

            JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                    .build();
            JsonSchema schema = factory.getSchema(schemaInputStream, InputFormat.YAML,
                    SchemaValidatorsConfig.builder()
                            .locale(LocaleContext.getLocale())
                            .pathType(PathType.JSON_PATH)
                            .build());
            Set<ValidationMessage> errors = schema.validate(jsonData);

            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                errors.forEach(error -> {
                    if (!sb.isEmpty()) {
                        sb.append(System.lineSeparator());
                    }
                    sb.append(error.getMessage());
                });
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_VALIDATE_ERROR.getErrorCode(), sb.toString()).build();
            }

            List<String> semanticErrorMessage = new ArrayList<>();
            DeviceTemplateModel deviceTemplateModel = parse(deviceTemplateContent);
            List<DeviceTemplateModel.Definition.InputJsonObject> deviceIdInputJsonObjects = getDeviceIdInputJsonObjects(deviceTemplateModel);
            if (deviceIdInputJsonObjects.size() != 1) {
                semanticErrorMessage.add(messageSource.getMessage(ValidationConstants.ERROR_MESSAGE_CODE_DEVICE_ID));
            }

            List<DeviceTemplateModel.Definition.InputJsonObject> deviceNameInputJsonObjects = getDeviceNameInputJsonObjects(deviceTemplateModel);
            if (deviceNameInputJsonObjects.size() > 1) {
                semanticErrorMessage.add(messageSource.getMessage(ValidationConstants.ERROR_MESSAGE_CODE_DEVICE_NAME));
            }

            if (!semanticErrorMessage.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                semanticErrorMessage.forEach(errorMessage -> {
                    if (!sb.isEmpty()) {
                        sb.append(System.lineSeparator());
                    }
                    sb.append(errorMessage);
                });
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_VALIDATE_ERROR.getErrorCode(), sb.toString()).build();
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else if (e instanceof ScannerException || e instanceof ParserException) {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_VALIDATE_ERROR.getErrorCode(), messageSource.getMessage(ValidationConstants.ERROR_MESSAGE_CODE_YAML_SYNTAX)).build();
            } else {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_VALIDATE_ERROR.getErrorCode(), messageSource.getMessage(ValidationConstants.ERROR_MESSAGE_CODE_SERVER_ERROR)).detailMessage(e.getMessage()).build();
            }
        }
    }

    private static class ValidationConstants {
        public static final String ERROR_MESSAGE_CODE_DEVICE_ID = "device-template-service.error.message.validation.device.id";
        public static final String ERROR_MESSAGE_CODE_DEVICE_NAME = "device-template-service.error.message.validation.device.name";
        public static final String ERROR_MESSAGE_CODE_YAML_SYNTAX = "device-template-service.error.message.validation.yaml.syntax";
        public static final String ERROR_MESSAGE_CODE_SERVER_ERROR = "device-template-service.error.message.validation.server.error";
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, Object data) {
        try {
            return input(integration, deviceTemplateId, null, data, null);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    @Override
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, Object data, Map<String, Object> codecArgContext) {
        try {
            return input(integration, deviceTemplateId, null, data, codecArgContext);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DeviceTemplateInputResult input(String deviceKey, Object data, Map<String, Object> codecArgContext) {
        try {
            return input(null, null, deviceKey, data, codecArgContext);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    private DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String deviceKey, Object data, Map<String, Object> codecArgContext) throws Exception {
        DeviceTemplateInputResult result = new DeviceTemplateInputResult();
        // Either (integration, deviceTemplateId) or deviceKey must be provided
        if ((integration == null || deviceTemplateId == null) && deviceKey == null) {
            throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorMessage()).build();
        }

        DeviceTemplate deviceTemplate;
        Device device = null;
        if (deviceTemplateId != null) {
            deviceTemplate = deviceTemplateServiceProvider.findById(deviceTemplateId);
        } else {
            device = deviceServiceProvider.findByKey(deviceKey);
            if (device == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_NOT_FOUND.getErrorMessage()).build();
            }

            String deviceTemplateKey = device.getTemplate();
            deviceTemplate = deviceTemplateServiceProvider.findByKey(deviceTemplateKey);
        }

        String deviceTemplateContent = getContentAndValidateDeviceTemplate(integration, deviceTemplate);
        DeviceTemplateModel deviceTemplateModel = parse(deviceTemplateContent);
        JsonNode jsonNode;
        if (data instanceof byte[] byteData) {
            DeviceCodecExecutorProvider deviceCodecExecutorProvider = codecExecutorServiceProvider.getDeviceCodecExecutor(deviceTemplate.getVendor(), deviceTemplate.getModel());
            if (deviceCodecExecutorProvider == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_DATA_DECODE_FAILED.getErrorCode(), ServerErrorCode.DEVICE_DATA_DECODE_FAILED.getErrorMessage()).build();
            }
            jsonNode = deviceCodecExecutorProvider.decode(byteData, codecArgContext);
        } else if (data instanceof String jsonData) {
            ObjectMapper mapper = new ObjectMapper();
            jsonNode = mapper.readTree(jsonData);
        } else if (data instanceof JsonNode jsonData){
            jsonNode = jsonData;
        } else {
            throw ServiceException.with(ServerErrorCode.DEVICE_DATA_UNKNOWN_TYPE.getErrorCode(), ServerErrorCode.DEVICE_DATA_UNKNOWN_TYPE.getErrorMessage()).build();
        }

        Map<String, JsonNode> flatJsonDataMap = new HashMap<>();
        flattenJsonData(jsonNode, flatJsonDataMap, "");

        Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap = new HashMap<>();
        flattenJsonInputDescription(deviceTemplateModel.getDefinition().getInput().getProperties(), flatJsonInputDescriptionMap, "");

        // Validate json data
        validateJsonData(flatJsonDataMap, flatJsonInputDescriptionMap);

        if (device == null) {
            String deviceIdKey = getDeviceIdKey(flatJsonInputDescriptionMap);
            String deviceNameKey = getDeviceNameKey(flatJsonInputDescriptionMap);

            if (deviceIdKey == null || flatJsonDataMap.get(deviceIdKey) == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_ID_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_ID_NOT_FOUND.formatMessage(deviceIdKey)).build();
            }
            String deviceId = flatJsonDataMap.get(deviceIdKey).asText();
            String deviceName = (deviceNameKey == null || flatJsonDataMap.get(deviceNameKey) == null) ? deviceId : flatJsonDataMap.get(deviceNameKey).asText();

            // Build device and device entities
            device = buildDeviceAndDeviceEntities(integration, deviceId, deviceName, deviceTemplate, deviceTemplateModel);

            if (deviceTemplateModel.getBlueprint() != null) {
                // Save device
                deviceServiceProvider.save(device);

                // Create device blueprint
                createDeviceBlueprint(device, deviceTemplate.getVendor(), deviceTemplateModel, BlueprintCreationStrategy.Optional);
                result.setDeviceAutoSaved(true);
            }
        }

        Map<String, Entity> flatDeviceEntityMap = new HashMap<>();
        flattenDeviceEntities(device.getEntities(), flatDeviceEntityMap, "");

        // Build device entity values payload
        ExchangePayload payload = buildDeviceEntityValuesPayload(flatJsonDataMap, flatJsonInputDescriptionMap, flatDeviceEntityMap);

        result.setDevice(device);
        result.setPayload(payload);
        return result;
    }

    private String getDeviceIdKey(Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap) {
        return flatJsonInputDescriptionMap.entrySet().stream().filter(entry -> entry.getValue().isDeviceId()).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private String getDeviceNameKey(Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap) {
        return flatJsonInputDescriptionMap.entrySet().stream().filter(entry -> entry.getValue().isDeviceName()).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private List<DeviceTemplateModel.Definition.InputJsonObject> getDeviceIdInputJsonObjects(DeviceTemplateModel deviceTemplateModel) {
        return getDeviceIdInputJsonObjects(deviceTemplateModel.getDefinition().getInput().getProperties());
    }

    private List<DeviceTemplateModel.Definition.InputJsonObject> getDeviceIdInputJsonObjects(List<DeviceTemplateModel.Definition.InputJsonObject> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        }

        List<DeviceTemplateModel.Definition.InputJsonObject> deviceIdInputJsonObjects = new ArrayList<>();
        for (DeviceTemplateModel.Definition.InputJsonObject property : properties) {
            if (DeviceTemplateModel.JsonType.OBJECT.equals(property.getType())) {
                List<DeviceTemplateModel.Definition.InputJsonObject> childDeviceIdInputJsonObjects = getDeviceIdInputJsonObjects(property.getProperties());
                if (!CollectionUtils.isEmpty(childDeviceIdInputJsonObjects)) {
                    deviceIdInputJsonObjects.addAll(childDeviceIdInputJsonObjects);
                }
            } else {
                if (property.isDeviceId()) {
                    deviceIdInputJsonObjects.add(property);
                }
            }
        }
        return deviceIdInputJsonObjects;
    }

    private List<DeviceTemplateModel.Definition.InputJsonObject> getDeviceNameInputJsonObjects(DeviceTemplateModel deviceTemplateModel) {
        return getDeviceNameInputJsonObjects(deviceTemplateModel.getDefinition().getInput().getProperties());
    }

    private List<DeviceTemplateModel.Definition.InputJsonObject> getDeviceNameInputJsonObjects(List<DeviceTemplateModel.Definition.InputJsonObject> properties) {
        if (CollectionUtils.isEmpty(properties)) {
            return Collections.emptyList();
        }

        List<DeviceTemplateModel.Definition.InputJsonObject> deviceIdInputJsonObjects = new ArrayList<>();
        for (DeviceTemplateModel.Definition.InputJsonObject property : properties) {
            if (DeviceTemplateModel.JsonType.OBJECT.equals(property.getType())) {
                List<DeviceTemplateModel.Definition.InputJsonObject> childDeviceIdInputJsonObjects = getDeviceNameInputJsonObjects(property.getProperties());
                if (!CollectionUtils.isEmpty(childDeviceIdInputJsonObjects)) {
                    deviceIdInputJsonObjects.addAll(childDeviceIdInputJsonObjects);
                }
            } else {
                if (property.isDeviceName()) {
                    deviceIdInputJsonObjects.add(property);
                }
            }
        }
        return deviceIdInputJsonObjects;
    }

    @Transactional(rollbackFor = Exception.class)
    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload) {
        try {
            return output(deviceKey, payload, null);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload, Map<String, Object> codecArgContext) {
        try {
            DeviceTemplateOutputResult result = new DeviceTemplateOutputResult();
            Device device = deviceServiceProvider.findByKey(deviceKey);
            if (device == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_NOT_FOUND.getErrorMessage()).build();
            }

            DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByKey(device.getTemplate());
            String deviceTemplateContent = getContentAndValidateDeviceTemplate(null, deviceTemplate);
            DeviceTemplateModel deviceTemplateModel = parse(deviceTemplateContent);
            if (deviceTemplateModel.getDefinition().getOutput() == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_DEFINITION_OUTPUT_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_DEFINITION_OUTPUT_NOT_FOUND.getErrorMessage()).build();
            }

            JsonNode outputData = buildJsonNode(deviceTemplateModel.getDefinition().getOutput(), deviceKey, payload);

            DeviceCodecExecutorProvider deviceCodecExecutorProvider = codecExecutorServiceProvider.getDeviceCodecExecutor(deviceTemplate.getVendor(), deviceTemplate.getModel());
            if (deviceCodecExecutorProvider == null) {
                result.setOutput(outputData);
                return result;
            }

            byte[] encodedData = deviceCodecExecutorProvider.encode(outputData, codecArgContext);
            result.setOutput(encodedData);
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    private JsonNode buildJsonNode(DeviceTemplateModel.Definition.Output outputRoot, String deviceKey, ExchangePayload payload) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        outputRoot.getProperties().forEach(outputJsonObject -> {
            JsonNode jsonNode = parseJsonNode(outputJsonObject, deviceKey, payload, "");
            if (jsonNode != null) {
                rootNode.set(outputJsonObject.getKey(), jsonNode);
            }
        });
        return rootNode;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Device createDevice(String integration, Long deviceTemplateId, String deviceIdentifier, String deviceName) {
        try {
            DeviceTemplate deviceTemplate = getAndValidateDeviceTemplate(integration, deviceTemplateId);
            return createDevice(integration,
                    null,
                    deviceTemplate,
                    deviceIdentifier,
                    deviceName,
                    false,
                    null,
                    BlueprintCreationStrategy.Never);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Device createDevice(String integration,
                               String vendor,
                               String model,
                               String deviceIdentifier,
                               String deviceName,
                               BiFunction<Device, Map<String, Object>, Boolean> beforeSaveDevice) {
        try {
            return createDevice(integration,
                    vendor,
                    model,
                    deviceIdentifier,
                    deviceName,
                    beforeSaveDevice,
                    BlueprintCreationStrategy.Optional);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Device createDevice(String integration,
                               String vendor,
                               String model,
                               String deviceIdentifier,
                               String deviceName,
                               BiFunction<Device, Map<String, Object>, Boolean> beforeSaveDevice,
                               BlueprintCreationStrategy strategy) {
        try {
            DeviceTemplate deviceTemplate = getDeviceTemplate(vendor, model);
            return createDevice(integration,
                    vendor,
                    deviceTemplate,
                    deviceIdentifier,
                    deviceName,
                    true,
                    beforeSaveDevice,
                    strategy);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    private Device createDevice(String integration,
                               String vendor,
                               DeviceTemplate deviceTemplate,
                               String deviceIdentifier,
                               String deviceName,
                               boolean shouldSaveDevice,
                               BiFunction<Device, Map<String, Object>, Boolean> beforeSaveDevice,
                               BlueprintCreationStrategy strategy) {
        String deviceTemplateContent = getContentAndValidateDeviceTemplate(integration, deviceTemplate);

        DeviceTemplateModel deviceTemplateModel = parse(deviceTemplateContent);

        // Build device and device entities
        Device device = buildDeviceAndDeviceEntities(integration, deviceIdentifier, deviceName, deviceTemplate, deviceTemplateModel);

        if (beforeSaveDevice != null && !beforeSaveDevice.apply(device, deviceTemplateModel.getMetadata())) {
            return null;
        }

        if (!shouldSaveDevice) {
            return device;
        }

        // Save device
        deviceServiceProvider.save(device);

        // Create device blueprint
        createDeviceBlueprint(device, vendor, deviceTemplateModel, strategy);
        return device;
    }

    private Device buildDeviceAndDeviceEntities(String integration, String deviceIdentifier, String deviceName, DeviceTemplate deviceTemplate, DeviceTemplateModel deviceTemplateModel) {
        // Build device
        Device device = buildDevice(integration, deviceIdentifier, deviceName, deviceTemplate.getKey());

        // Build device entities
        List<Entity> deviceEntities = buildDeviceEntities(integration, device.getKey(), deviceTemplateModel.getInitialEntities());
        device.setEntities(deviceEntities);
        return device;
    }

    private void createDeviceBlueprint(Device device, String vendor, DeviceTemplateModel deviceTemplateModel, BlueprintCreationStrategy strategy) {
        if (strategy == null || strategy == BlueprintCreationStrategy.Never) {
            return;
        }

        DeviceTemplateModel.Blueprint blueprint = deviceTemplateModel.getBlueprint();
        if (blueprint == null) {
            if (strategy == BlueprintCreationStrategy.Optional) {
                return;
            }

            throw ServiceException.with(ServerErrorCode.DEVICE_BLUEPRINT_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_BLUEPRINT_NOT_FOUND.getErrorMessage()).build();
        }

        if (!blueprint.validate()) {
            throw ServiceException.with(ServerErrorCode.DEVICE_BLUEPRINT_CREATION_FAILED.getErrorCode(), ServerErrorCode.DEVICE_BLUEPRINT_CREATION_FAILED.getErrorMessage()).build();
        }

        if (deviceBlueprintMappingServiceProvider.getBlueprintIdByDeviceId(device.getId()) != null) {
            return;
        }

        BlueprintDeviceVendor deviceVendor = blueprintLibraryResourceProvider.getDeviceVendor(vendor);
        String workDir = deviceVendor.getWorkDir();
        String blueprintPath = blueprintLibraryResourceProvider.getResourcePath(workDir, blueprint.getDir());
        TemplateLoader loader = new DefaultTemplateLoader(blueprintPath);
        Map<String, Object> blueprintValues = buildBlueprintValues(device, blueprint.getValues());
        Long blueprintId = blueprintServiceProvider.deployBlueprint(loader, blueprintValues);
        if (blueprintId == null) {
            throw ServiceException.with(ServerErrorCode.DEVICE_BLUEPRINT_CREATION_FAILED.getErrorCode(), ServerErrorCode.DEVICE_BLUEPRINT_CREATION_FAILED.getErrorMessage()).build();
        }

        deviceBlueprintMappingServiceProvider.saveMapping(device.getId(), blueprintId);
    }

    private DeviceTemplate getDeviceTemplate(String vendor, String model) {
        String deviceTemplateIdentifier = DeviceTemplateHelper.getTemplateIdentifier(vendor, model);
        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByVendorAndModel(vendor, model);
        if (deviceTemplate == null) {
            deviceTemplate = new DeviceTemplateBuilder(IntegrationConstants.SYSTEM_INTEGRATION_ID)
                    .name(DeviceTemplateHelper.getTemplateName(vendor, model))
                    .identifier(deviceTemplateIdentifier)
                    .vendor(vendor)
                    .model(model)
                    .build();
            deviceTemplateServiceProvider.save(deviceTemplate);
        }
        return deviceTemplate;
    }

    private Map<String, Object> buildBlueprintValues(Device device, Map<String, DeviceTemplateModel.Blueprint.Value> values) {
        Map<String, Object> blueprintValues = new HashMap<>();
        values.forEach((key, value) -> {
            Object valueObject;
            if (DeviceTemplateModel.Blueprint.Value.TYPE_ENTITY.equals(value.getType())) {
                valueObject = entityServiceProvider.findByKey(device.getKey() + "." + value.getIdentifier());
            } else {
                valueObject = value.getValue();
            }
            blueprintValues.put(key, valueObject);
        });
        return blueprintValues;
    }

    private DeviceTemplate getAndValidateDeviceTemplate(String integration, Long deviceTemplateId) {
        if (integrationServiceProvider.getIntegration(integration) == null) {
            throw ServiceException.with(ServerErrorCode.INTEGRATION_NOT_FOUND.getErrorCode(), ServerErrorCode.INTEGRATION_NOT_FOUND.getErrorMessage()).build();
        }

        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findById(deviceTemplateId);
        if (deviceTemplate == null) {
            throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorMessage()).build();
        }

        if (!validate(deviceTemplate.getContent())) {
            return null;
        }

        return deviceTemplate;
    }

    private String getContentAndValidateDeviceTemplate(String integration, DeviceTemplate deviceTemplate) {
        if (integration != null && integrationServiceProvider.getIntegration(integration) == null) {
            throw ServiceException.with(ServerErrorCode.INTEGRATION_NOT_FOUND.getErrorCode(), ServerErrorCode.INTEGRATION_NOT_FOUND.getErrorMessage()).build();
        }

        if (deviceTemplate == null) {
            throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorMessage()).build();
        }

        String content;
        String vendor = deviceTemplate.getVendor();
        String model = deviceTemplate.getModel();
        if (vendor != null && model != null) {
            content = blueprintLibraryResourceProvider.getDeviceTemplateContent(vendor, model);
        } else {
            content = deviceTemplate.getContent();
        }

        if (!validate(content)) {
            return null;
        }

        return content;
    }

    public DeviceTemplateModel parse(String deviceTemplateContent) {
        try {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            return objectMapper.readValue(deviceTemplateContent, DeviceTemplateModel.class);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    private JsonNode parseJsonNode(DeviceTemplateModel.Definition.OutputJsonObject outputJsonObject, String deviceKey, ExchangePayload payload, String parentKey) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        String currentKey = parentKey + outputJsonObject.getKey();
        if (DeviceTemplateModel.JsonType.OBJECT.equals(outputJsonObject.getType())) {
            if (outputJsonObject.getProperties() != null) {
                for (DeviceTemplateModel.Definition.OutputJsonObject child : outputJsonObject.getProperties()) {
                    JsonNode childJsonNode = parseJsonNode(child, deviceKey, payload, currentKey + ".");
                    if (childJsonNode != null) {
                        jsonNode.set(child.getKey(), childJsonNode);
                    }
                }
            }
        } else {
            if (outputJsonObject.getValue() != null) {
                return mapper.valueToTree(outputJsonObject.getValue());
            } else {
                if (outputJsonObject.getEntityMapping() == null) {
                    return null;
                }
                String entityKey = deviceKey + "." + outputJsonObject.getEntityMapping();
                if (!payload.containsKey(entityKey)) {
                    return null;
                }
                Object value = payload.get(entityKey);
                validateEntityValue(currentKey, entityKey, outputJsonObject.getType(), value);
                return mapper.valueToTree(value);
            }
        }
        return jsonNode;
    }

    private void validateEntityValue(String currentKey, String entityKey, DeviceTemplateModel.JsonType definitionType, Object value) {
        Class<?> clazz = value.getClass();
        if (!isMatchType(definitionType, value)) {
            throw ServiceException.with(ServerErrorCode.DEVICE_ENTITY_VALUE_VALIDATE_ERROR.getErrorCode(),
                    MessageFormat.format("Invalid value type for json key ''{0}'': requires type {1}, but entity key ''{2}'' provides type {3} with value {4}",
                            currentKey,
                            definitionType.getTypeName(),
                            entityKey,
                            clazz.getSimpleName(),
                            value instanceof String ? "'" + value + "'" : value)).build();
        }
    }

    private boolean isMatchType(DeviceTemplateModel.JsonType definitionType, Object value) {
        return switch (definitionType) {
            case DOUBLE -> ValidationUtils.isNumber(value.toString());
            case LONG -> ValidationUtils.isInteger(value.toString());
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
            default -> false;
        };
    }

    private ExchangePayload buildDeviceEntityValuesPayload(Map<String, JsonNode> flatJsonDataMap,
                                                                  Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap,
                                                                  Map<String, Entity> flatDeviceEntityMap) {
        ExchangePayload payload = new ExchangePayload();
        for (String key : flatJsonDataMap.keySet()) {
            JsonNode jsonNode = flatJsonDataMap.get(key);
            DeviceTemplateModel.Definition.InputJsonObject inputJsonObject = flatJsonInputDescriptionMap.get(key);
            if (inputJsonObject == null) {
                continue;
            }
            if (inputJsonObject.getType().equals(DeviceTemplateModel.JsonType.OBJECT)) {
                continue;
            }
            String entityMapping = inputJsonObject.getEntityMapping();
            Object value = getJsonValue(jsonNode, inputJsonObject.getType());
            Entity entity = flatDeviceEntityMap.get(entityMapping);
            if (entity == null) {
                continue;
            }
            payload.put(entity.getKey(), value);
        }
        return payload;
    }

    private Object getJsonValue(JsonNode jsonNode, DeviceTemplateModel.JsonType jsonType) {
        return switch (jsonType) {
            case DOUBLE -> jsonNode.asDouble();
            case LONG -> jsonNode.asLong();
            case BOOLEAN -> jsonNode.asBoolean();
            case STRING -> jsonNode.asText();
            default -> null;
        };
    }

    private void validateJsonData(Map<String, JsonNode> flatJsonDataMap, Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap) {
        for (String key : flatJsonInputDescriptionMap.keySet()) {
            DeviceTemplateModel.Definition.InputJsonObject inputJsonObject = flatJsonInputDescriptionMap.get(key);
            if (inputJsonObject.isRequired() && !flatJsonDataMap.containsKey(key)) {
                throw ServiceException.with(ServerErrorCode.JSON_VALIDATE_ERROR.getErrorCode(), MessageFormat.format("Json validate failed. Required key {0} is missing", key)).build();
            }
        }

        for (String key : flatJsonDataMap.keySet()) {
            JsonNode jsonNode = flatJsonDataMap.get(key);
            if (flatJsonInputDescriptionMap.containsKey(key)) {
                DeviceTemplateModel.Definition.InputJsonObject inputJsonObject = flatJsonInputDescriptionMap.get(key);
                if (!isMatchType(inputJsonObject, jsonNode)) {
                    throw ServiceException.with(ServerErrorCode.JSON_VALIDATE_ERROR.getErrorCode(), MessageFormat.format("Json validate failed. Json key {0} requires type {1}", key, inputJsonObject.getType().getTypeName())).build();
                }
            }
        }
    }

    private boolean isMatchType(DeviceTemplateModel.Definition.InputJsonObject inputJsonObject, JsonNode jsonNode) {
        return switch (inputJsonObject.getType()) {
            case DOUBLE -> jsonNode.isFloat() || jsonNode.isDouble() || jsonNode.isInt() || jsonNode.isLong();
            case LONG -> jsonNode.isInt() || jsonNode.isLong();
            case BOOLEAN -> jsonNode.isBoolean();
            case STRING -> jsonNode.isTextual();
            case OBJECT -> jsonNode.isObject();
        };
    }

    private void flattenDeviceEntities(List<Entity> deviceEntities, Map<String, Entity> flatDeviceEntityMap, String parentKey) {
        if (deviceEntities == null) {
            return;
        }
        for (Entity entity : deviceEntities) {
            String key = parentKey + entity.getIdentifier();
            flatDeviceEntityMap.put(key, entity);
            if (!CollectionUtils.isEmpty(entity.getChildren())) {
                flattenDeviceEntities(entity.getChildren(), flatDeviceEntityMap, key + ".");
            }
        }
    }

    private void flattenJsonData(JsonNode jsonData, Map<String, JsonNode> flatJsonDataMap, String parentKey) {
        for (Iterator<Map.Entry<String, JsonNode>> it = jsonData.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = parentKey + entry.getKey();
            JsonNode value = entry.getValue();
            flatJsonDataMap.put(key, value);
            if (value.isObject()) {
                flattenJsonData(value, flatJsonDataMap, key + ".");
            }
        }
    }

    private void flattenJsonInputDescription(List<DeviceTemplateModel.Definition.InputJsonObject> properties, Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap, String parentKey) {
        for (DeviceTemplateModel.Definition.InputJsonObject property : properties) {
            String key = parentKey + property.getKey();
            flatJsonInputDescriptionMap.put(key, property);
            if (property.getType().equals(DeviceTemplateModel.JsonType.OBJECT) && property.getProperties() != null) {
                flattenJsonInputDescription(property.getProperties(), flatJsonInputDescriptionMap, key + ".");
            }
        }
    }

    protected Device buildDevice(String integration, String deviceId, String deviceName, String deviceTemplateKey) {
        return new DeviceBuilder(integration)
                .name(deviceName)
                .template(deviceTemplateKey)
                .identifier(deviceId)
                .additional(Map.of("deviceId", deviceId))
                .build();
    }

    protected List<Entity> buildDeviceEntities(String integration, String deviceKey, List<EntityConfig> initialEntities) {
        if (CollectionUtils.isEmpty(initialEntities)) {
            return null;
        }
        List<Entity> entities = initialEntities.stream().map(entityConfig -> {
            Entity entity = entityConfig.toEntity();
            entity.setIntegrationId(integration);
            entity.setDeviceKey(deviceKey);
            return entity;
        }).toList();
        entities.forEach(entity -> entity.initializeProperties(integration, deviceKey));
        return entities;
    }
}
