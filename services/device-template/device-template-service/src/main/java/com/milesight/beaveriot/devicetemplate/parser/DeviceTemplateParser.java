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
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateParserFacade;
import com.milesight.beaveriot.devicetemplate.parser.model.DeviceTemplateModel;
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
    private final static String DEVICE_ID_KEY = "device_id";
    private final DeviceServiceProvider deviceServiceProvider;
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;

    public DeviceTemplateParser(DeviceServiceProvider deviceServiceProvider, DeviceTemplateServiceProvider deviceTemplateServiceProvider) {
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
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Device template empty").build();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonData = mapper.readTree(mapper.writeValueAsBytes(loadedYaml));

            InputStream schemaInputStream = DeviceTemplateParser.class.getClassLoader()
                    .getResourceAsStream("template/device_template_schema.json");
            if (schemaInputStream == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Device template schema not found").build();
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
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), sb.toString()).build();
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public DeviceTemplateInputResult input(String integration, Long deviceTemplateId, String jsonData) {
        DeviceTemplateInputResult result = new DeviceTemplateInputResult();
        try {
            DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findById(deviceTemplateId);
            if (deviceTemplate == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Device template not found").build();
            }

            if (!validate(deviceTemplate.getContent())) {
                return result;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonData);

            if (jsonNode.get(DEVICE_ID_KEY) == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Key device_id not found.").build();
            }
            DeviceTemplateModel deviceTemplateModel = parseDeviceTemplate(deviceTemplate.getContent());

            Map<String, JsonNode> flatJsonDataMap = new HashMap<>();
            flattenJsonData(jsonNode, flatJsonDataMap, "");

            Map<String, DeviceTemplateModel.Definition.InputJsonObject> flatJsonInputDescriptionMap = new HashMap<>();
            flattenJsonInputDescription(deviceTemplateModel.getDefinition().getInput().getProperties(), flatJsonInputDescriptionMap, "");

            // Validate json data
            validateJsonData(flatJsonDataMap, flatJsonInputDescriptionMap);

            // Build device
            String deviceId = jsonNode.get(DEVICE_ID_KEY).asText();
            Device device = buildDevice(integration, deviceId, deviceTemplate.getKey());

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
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload) {
        try {
            DeviceTemplateOutputResult result = new DeviceTemplateOutputResult();
            Device device = deviceServiceProvider.findByKey(deviceKey);
            if (device == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Device not found").build();
            }

            DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByKey(device.getTemplate());
            if (deviceTemplate == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Device template not found").build();
            }

            DeviceTemplateModel deviceTemplateModel = parseDeviceTemplate(deviceTemplate.getContent());
            if (deviceTemplateModel.getDefinition().getOutput() == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Device template definition of output not found").build();
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            deviceTemplateModel.getDefinition().getOutput().getProperties().forEach(outputJsonObject -> {
                JsonNode jsonNode = parseJsonNode(outputJsonObject, deviceKey, payload);
                if (jsonNode != null) {
                    rootNode.set(outputJsonObject.getKey(), jsonNode);
                }
            });

            result.setOutput(mapper.writeValueAsString(rootNode));
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    private JsonNode parseJsonNode(DeviceTemplateModel.Definition.OutputJsonObject outputJsonObject, String deviceKey, ExchangePayload payload) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        if (DeviceTemplateModel.JsonType.OBJECT.equals(outputJsonObject.getType())) {
            if (outputJsonObject.getProperties() != null) {
                for (DeviceTemplateModel.Definition.OutputJsonObject child : outputJsonObject.getProperties()) {
                    JsonNode childJsonNode = parseJsonNode(child, deviceKey, payload);
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
                return mapper.valueToTree(value);
            }
        }
        return jsonNode;
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
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), MessageFormat.format("Json validate failed. Required key {0} is missing", key)).build();
            }
        }

        for (String key : flatJsonDataMap.keySet()) {
            JsonNode jsonNode = flatJsonDataMap.get(key);
            if (flatJsonInputDescriptionMap.containsKey(key)) {
                DeviceTemplateModel.Definition.InputJsonObject inputJsonObject = flatJsonInputDescriptionMap.get(key);
                if (!isMatchType(inputJsonObject, jsonNode)) {
                    throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), MessageFormat.format("Json validate failed. Json key {0} required type {1}", key, inputJsonObject.getType().getTypeName())).build();
                }
            }
        }
    }

    private boolean isMatchType(DeviceTemplateModel.Definition.InputJsonObject inputJsonObject, JsonNode jsonNode) {
        return switch (inputJsonObject.getType()) {
            case DOUBLE -> jsonNode.isDouble();
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

    private DeviceTemplateModel parseDeviceTemplate(String deviceTemplateContent) {
        try {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            return objectMapper.readValue(deviceTemplateContent, DeviceTemplateModel.class);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    protected Device buildDevice(String integration, String deviceId, String deviceTemplateKey) {
        return new DeviceBuilder(integration)
                .name(deviceId)
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
