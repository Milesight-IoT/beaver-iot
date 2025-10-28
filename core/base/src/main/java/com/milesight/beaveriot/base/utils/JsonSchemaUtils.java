package com.milesight.beaveriot.base.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.exception.JsonSchemaValidationException;
import com.networknt.schema.ExecutionConfig;
import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.serialization.DefaultNodeReader;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Slf4j
public class JsonSchemaUtils {

    private static final SchemaRegistry DEFAULT_JSON_SCHEMA_REGISTRY = SchemaRegistry.withDialect(Dialects.getDraft202012(),
            builder -> builder.nodeReader(DefaultNodeReader.Builder::locationAware));

    /**
     * validate json data
     *
     * @param jsonSchema json schema
     * @param data       data to validate
     * @param locale     locale for error message
     * @throws JsonSchemaValidationException errors when validating json schema or data
     */
    @NonNull
    public static void validate(JsonNode jsonSchema, JsonNode data, Locale locale) throws JsonSchemaValidationException {
        try {
            var schema = DEFAULT_JSON_SCHEMA_REGISTRY.getSchema(JsonUtils.toJSON(jsonSchema), InputFormat.JSON);
            var errors = schema.validate(JsonUtils.toJSON(data), InputFormat.JSON, executionContext ->
                    executionContext.executionConfig(withDefaultExecutionConfig(locale)));
            if (errors.isEmpty()) {
                return;
            }
            throw new JsonSchemaValidationException("Json data is not valid.", getDetails(errors));
        } catch (SchemaException e) {
            throw new JsonSchemaValidationException("Json data validation failed.", e, getDetails(e.getErrors()));
        }
    }

    @NonNull
    private static Consumer<ExecutionConfig.Builder> withDefaultExecutionConfig(Locale locale) {
        return executionConfig -> executionConfig.locale(locale).formatAssertionsEnabled(true);
    }

    @NonNull
    private static List<JsonNode> getDetails(List<com.networknt.schema.Error> errors) {
        return errors.stream().map(error -> JsonUtils.cast(error, JsonNode.class)).toList();
    }

    private JsonSchemaUtils() {
        throw new UnsupportedOperationException();
    }

}
