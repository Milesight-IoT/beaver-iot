package com.milesight.beaveriot.devicetemplate.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;
import com.milesight.beaveriot.devicetemplate.enums.ServerErrorCode;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateParserFacade;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

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

    public DeviceTemplateParser(IntegrationServiceProvider integrationServiceProvider, DeviceServiceProvider deviceServiceProvider, DeviceTemplateServiceProvider deviceTemplateServiceProvider) {
        this.integrationServiceProvider = integrationServiceProvider;
        this.deviceServiceProvider = deviceServiceProvider;
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
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
                    .objectMapper(mapper)
                    .build();
            JsonSchema schema = factory.getSchema(schemaInputStream);
            Set<ValidationMessage> errors = schema.validate(jsonData);

            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Validate error:").append(System.lineSeparator());
                errors.forEach(error -> sb.append(error.getMessage()).append(System.lineSeparator()));
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_VALIDATE_ERROR.getErrorCode(), sb.toString()).build();
            }

            List<String> semanticErrorMessage = new ArrayList<>();
            DeviceTemplateModel deviceTemplateModel = parse(deviceTemplateContent);
            List<DeviceTemplateModel.Definition.InputJsonObject> deviceIdInputJsonObjects = getDeviceIdInputJsonObjects(deviceTemplateModel);
            if (deviceIdInputJsonObjects.size() != 1) {
                semanticErrorMessage.add(ValidationConstants.ERROR_MESSAGE_DEVICE_ID);
            }

            List<DeviceTemplateModel.Definition.InputJsonObject> deviceNameInputJsonObjects = getDeviceNameInputJsonObjects(deviceTemplateModel);
            if (deviceNameInputJsonObjects.size() > 1) {
                semanticErrorMessage.add(ValidationConstants.ERROR_MESSAGE_DEVICE_NAME);
            }

            if (!semanticErrorMessage.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Validate error:").append(System.lineSeparator());
                semanticErrorMessage.forEach(errorMessage -> sb.append(errorMessage).append(System.lineSeparator()));
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_VALIDATE_ERROR.getErrorCode(), sb.toString()).build();
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    private static class ValidationConstants {
        public static final String ERROR_MESSAGE_DEVICE_ID = "$.definition.input.properties: exactly one property must serve as a device id identifier (is_device_id = true)";
        public static final String ERROR_MESSAGE_DEVICE_NAME = "$.definition.input.properties: at most one property can serve as a device name identifier (is_device_name = true)";
    }

    @Transactional(rollbackFor = Exception.class)
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String jsonData) {
        DeviceTemplateInputResult result = new DeviceTemplateInputResult();
        try {
            DeviceTemplate deviceTemplate = getAndValidateDeviceTemplate(integration, deviceTemplateId);
            if (deviceTemplate == null) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonData);

            DeviceTemplateModel deviceTemplateModel = parse(deviceTemplate.getContent());
            String deviceIdKey = getDeviceIdKey(deviceTemplateModel);
            String deviceNameKey = getDeviceNameKey(deviceTemplateModel);

            if (deviceIdKey == null || jsonNode.get(deviceIdKey) == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_ID_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_ID_NOT_FOUND.formatMessage(deviceIdKey)).build();
            }

            Map<String, JsonNode> flatJsonDataMap = new HashMap<>();
            flattenJsonData(jsonNode, flatJsonDataMap, "");

            Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap = new HashMap<>();
            flattenJsonInputDescription(deviceTemplateModel.getDefinition().getInput().getProperties(), flatJsonInputDescriptionMap, "");

            // Validate json data
            validateJsonData(flatJsonDataMap, flatJsonInputDescriptionMap);

            // Build device
            String deviceId = jsonNode.get(deviceIdKey).asText();
            String deviceName = (deviceNameKey == null || jsonNode.get(deviceNameKey) == null) ? deviceId : jsonNode.get(deviceNameKey).asText();
            Device device = buildDevice(integration, deviceId, deviceName, deviceTemplate.getKey());

            // Build device entities
            List<Entity> deviceEntities = buildDeviceEntities(integration, device.getKey(), deviceTemplateModel.getInitialEntities());
            device.setEntities(deviceEntities);
            Map<String, Entity> flatDeviceEntityMap = new HashMap<>();
            flattenDeviceEntities(deviceEntities, flatDeviceEntityMap, "");

            // Build device entity values payload
            ExchangePayload payload = buildDeviceEntityValuesPayload(flatJsonDataMap, flatJsonInputDescriptionMap, flatDeviceEntityMap);

            result.setDevice(device);
            result.setPayload(payload);
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

    private String getDeviceIdKey(DeviceTemplateModel deviceTemplateModel) {
        List<DeviceTemplateModel.Definition.InputJsonObject> deviceIdInputJsonObjects = getDeviceIdInputJsonObjects(deviceTemplateModel);
        String deviceIdKey = null;
        if (!deviceIdInputJsonObjects.isEmpty()) {
            deviceIdKey = deviceIdInputJsonObjects.get(0).getKey();
        }
        return deviceIdKey;
    }

    private String getDeviceNameKey(DeviceTemplateModel deviceTemplateModel) {
        List<DeviceTemplateModel.Definition.InputJsonObject> deviceNameInputJsonObjects = getDeviceNameInputJsonObjects(deviceTemplateModel);
        String deviceNameKey = null;
        if (!deviceNameInputJsonObjects.isEmpty()) {
            deviceNameKey = deviceNameInputJsonObjects.get(0).getKey();
        }
        return deviceNameKey;
    }

    private List<DeviceTemplateModel.Definition.InputJsonObject> getDeviceIdInputJsonObjects(DeviceTemplateModel deviceTemplateModel) {
        return deviceTemplateModel.getDefinition().getInput().getProperties().stream().filter(
                DeviceTemplateModel.Definition.InputJsonObject::isDeviceId).toList();
    }

    private List<DeviceTemplateModel.Definition.InputJsonObject> getDeviceNameInputJsonObjects(DeviceTemplateModel deviceTemplateModel) {
        return deviceTemplateModel.getDefinition().getInput().getProperties().stream().filter(
                DeviceTemplateModel.Definition.InputJsonObject::isDeviceName).toList();
    }

    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload) {
        try {
            DeviceTemplateOutputResult result = new DeviceTemplateOutputResult();
            Device device = deviceServiceProvider.findByKey(deviceKey);
            if (device == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_NOT_FOUND.getErrorMessage()).build();
            }

            DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByKey(device.getTemplate());
            if (deviceTemplate == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_NOT_FOUND.getErrorMessage()).build();
            }

            DeviceTemplateModel deviceTemplateModel = parse(deviceTemplate.getContent());
            if (deviceTemplateModel.getDefinition().getOutput() == null) {
                throw ServiceException.with(ServerErrorCode.DEVICE_TEMPLATE_DEFINITION_OUTPUT_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_TEMPLATE_DEFINITION_OUTPUT_NOT_FOUND.getErrorMessage()).build();
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            deviceTemplateModel.getDefinition().getOutput().getProperties().forEach(outputJsonObject -> {
                JsonNode jsonNode = parseJsonNode(outputJsonObject, deviceKey, payload, "");
                if (jsonNode != null) {
                    rootNode.set(outputJsonObject.getKey(), jsonNode);
                }
            });

            result.setOutput(mapper.writeValueAsString(rootNode));
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

    @Override
    public Device createDevice(String integration, Long deviceTemplateId, String deviceId, String deviceName) {
        try {
            DeviceTemplate deviceTemplate = getAndValidateDeviceTemplate(integration, deviceTemplateId);
            if (deviceTemplate == null) {
                return null;
            }

            DeviceTemplateModel deviceTemplateModel = parse(deviceTemplate.getContent());
            // Build device
            Device device = buildDevice(integration, deviceId, deviceName, deviceTemplate.getKey());

            // Build device entities
            List<Entity> deviceEntities = buildDeviceEntities(integration, device.getKey(), deviceTemplateModel.getInitialEntities());
            device.setEntities(deviceEntities);
            return device;
        } catch (Exception e) {
            log.error(e.getMessage());
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
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
        DeviceTemplateModel.JsonType targetType = DeviceTemplateModel.JsonType.STRING;
        if (value instanceof Boolean) {
            targetType = DeviceTemplateModel.JsonType.BOOLEAN;
        } else if (value instanceof Double || value instanceof Float) {
            targetType = DeviceTemplateModel.JsonType.DOUBLE;
        } else if (value instanceof Long || value instanceof Integer) {
            targetType = DeviceTemplateModel.JsonType.LONG;
        }
        if (!definitionType.equals(targetType)) {
            throw ServiceException.with(ServerErrorCode.DEVICE_ENTITY_VALUE_VALIDATE_ERROR.getErrorCode(),
                    MessageFormat.format("Invalid value type for json key ''{0}'': requires type {1}, but entity key ''{2}'' provides type {3} with value {4}",
                            currentKey,
                            definitionType.getTypeName(),
                            entityKey,
                            targetType.getTypeName(),
                            targetType.equals(DeviceTemplateModel.JsonType.STRING) ? "'" + value + "'" : value)).build();
        }
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
