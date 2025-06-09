package com.milesight.beaveriot.devicetemplate.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.model.DeviceTemplateType;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;
import com.milesight.beaveriot.devicetemplate.parser.model.json.JsonDeviceTemplate;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

/**
 * author: Luxb
 * create: 2025/5/15 11:16
 **/
@Slf4j
@Service
public class JsonDeviceTemplateParser extends DeviceTemplateParser {
    private final static String DEVICE_ID_KEY = "device_id";
    private final DeviceTemplateType deviceTemplateType = DeviceTemplateType.JSON;
    private final EntityValueServiceProvider entityValueServiceProvider;

    public JsonDeviceTemplateParser(EntityValueServiceProvider entityValueServiceProvider) {
        this.entityValueServiceProvider = entityValueServiceProvider;
    }

    @Override
    public boolean validate(String deviceTemplateContent) {
        try {
            Yaml yaml = new Yaml();
            Object loadedYaml = yaml.load(deviceTemplateContent);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonData = mapper.readTree(mapper.writeValueAsBytes(loadedYaml));

            InputStream schemaInputStream = JsonDeviceTemplateParser.class.getClassLoader()
                    .getResourceAsStream("template/" + deviceTemplateType + "/device_template_schema.json");
            if (schemaInputStream == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "JSON Schema not found").build();
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

    @Override
    public DeviceTemplateDiscoverResponse discover(String integration, Object data, Long deviceTemplateId, String deviceTemplateContent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonData = mapper.readTree(data.toString());

            if (jsonData.get(DEVICE_ID_KEY) == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Key device_id not found.").build();
            }
            JsonDeviceTemplate jsonDeviceTemplate = parseJsonDeviceTemplate(deviceTemplateContent);

            Map<String, JsonNode> flatJsonDataMap = new HashMap<>();
            flattenJsonData(jsonData, flatJsonDataMap, "");

            Map<String, JsonDeviceTemplate.Definition.InputJsonObject> flatJsonInputDescriptionMap = new HashMap<>();
            flattenJsonInputDescription(jsonDeviceTemplate.getDefinition().getInput().getProperties(), flatJsonInputDescriptionMap, "");

            // Validate json data
            validateJsonData(flatJsonDataMap, flatJsonInputDescriptionMap);

            // Save device
            String deviceId = jsonData.get(DEVICE_ID_KEY).asText();
            Device device = saveDevice(integration, deviceId, deviceId, deviceTemplateId);

            // Save device entities
            List<Entity> deviceEntities = saveDeviceEntities(integration, device.getKey(), jsonDeviceTemplate.getInitialEntities());
            Map<String, Entity> flatDeviceEntityMap = new HashMap<>();
            flattenDeviceEntities(deviceEntities, flatDeviceEntityMap, "");

            // Save device entity values
            return saveDeviceEntityValues(flatJsonDataMap, flatJsonInputDescriptionMap, flatDeviceEntityMap);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }

    private DeviceTemplateDiscoverResponse saveDeviceEntityValues(Map<String, JsonNode> flatJsonDataMap,
                                        Map<String, JsonDeviceTemplate.Definition.InputJsonObject> flatJsonInputDescriptionMap,
                                        Map<String, Entity> flatDeviceEntityMap) {
        DeviceTemplateDiscoverResponse response = new DeviceTemplateDiscoverResponse();
        ExchangePayload payload = new ExchangePayload();
        for (String key : flatJsonDataMap.keySet()) {
            JsonNode jsonNode = flatJsonDataMap.get(key);
            JsonDeviceTemplate.Definition.InputJsonObject inputJsonObject = flatJsonInputDescriptionMap.get(key);
            if (inputJsonObject == null) {
                continue;
            }
            if (inputJsonObject.getType().equals(JsonDeviceTemplate.JsonType.OBJECT)) {
                continue;
            }
            String entityMapping = inputJsonObject.getEntityMapping();
            Object value = getJsonValue(jsonNode, inputJsonObject.getType());
            Entity entity = flatDeviceEntityMap.get(entityMapping);
            if (entity == null) {
                continue;
            }
            response.addEntity(entity.getName(), value);
            payload.put(entity.getKey(), value);
        }
        if (!payload.isEmpty()) {
            entityValueServiceProvider.saveValuesAndPublishSync(payload);
        }
        return response;
    }

    private Object getJsonValue(JsonNode jsonNode, JsonDeviceTemplate.JsonType jsonType) {
        return switch (jsonType) {
            case DOUBLE -> jsonNode.asDouble();
            case LONG -> jsonNode.asLong();
            case BOOLEAN -> jsonNode.asBoolean();
            case STRING -> jsonNode.asText();
            default -> null;
        };
    }

    private void validateJsonData(Map<String, JsonNode> flatJsonDataMap, Map<String, JsonDeviceTemplate.Definition.InputJsonObject> flatJsonInputDescriptionMap) {
        for (String key : flatJsonInputDescriptionMap.keySet()) {
            JsonDeviceTemplate.Definition.InputJsonObject inputJsonObject = flatJsonInputDescriptionMap.get(key);
            if (inputJsonObject.isRequired() && !flatJsonDataMap.containsKey(key)) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), MessageFormat.format("Json validate failed. Required key {0} is missing", key)).build();
            }
        }

        for (String key : flatJsonDataMap.keySet()) {
            JsonNode jsonNode = flatJsonDataMap.get(key);
            if (flatJsonInputDescriptionMap.containsKey(key)) {
                JsonDeviceTemplate.Definition.InputJsonObject inputJsonObject = flatJsonInputDescriptionMap.get(key);
                if (!isMatchType(inputJsonObject, jsonNode)) {
                    throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), MessageFormat.format("Json validate failed. Json key {0} required type {1}", key, inputJsonObject.getType().getTypeName())).build();
                }
            }
        }
    }

    private boolean isMatchType(JsonDeviceTemplate.Definition.InputJsonObject inputJsonObject, JsonNode jsonNode) {
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

    private void flattenJsonInputDescription(List<JsonDeviceTemplate.Definition.InputJsonObject> properties, Map<String, JsonDeviceTemplate.Definition.InputJsonObject> flatJsonInputDescriptionMap, String parentKey) {
        for (JsonDeviceTemplate.Definition.InputJsonObject property : properties) {
            String key = parentKey + property.getKey();
            flatJsonInputDescriptionMap.put(key, property);
            if (property.getType().equals(JsonDeviceTemplate.JsonType.OBJECT) && property.getProperties() != null) {
                flattenJsonInputDescription(property.getProperties(), flatJsonInputDescriptionMap, key + ".");
            }
        }
    }

    private JsonDeviceTemplate parseJsonDeviceTemplate(String deviceTemplateContent) {
        try {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            return objectMapper.readValue(deviceTemplateContent, JsonDeviceTemplate.class);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
        }
    }
}
