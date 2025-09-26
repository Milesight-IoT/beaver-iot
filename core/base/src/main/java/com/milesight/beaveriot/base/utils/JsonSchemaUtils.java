package com.milesight.beaveriot.base.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.milesight.beaveriot.base.exception.JsonSchemaValidationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.stream.StreamSupport;

@Slf4j
public class JsonSchemaUtils {

    private static final JsonSchemaFactory DEFAULT_JSON_SCHEMA_FACTORY = JsonSchemaFactory.byDefault();

    /**
     * validate json data
     *
     * @param jsonSchema json schema
     * @param data data to validate
     * @throws JsonSchemaValidationException errors when validating json schema or data
     */
    @NonNull
    public static void validate(JsonNode jsonSchema, JsonNode data) throws JsonSchemaValidationException {
        try {
            var report = DEFAULT_JSON_SCHEMA_FACTORY.getJsonSchema(jsonSchema)
                    .validate(data);
            if (report.isSuccess()) {
                return;
            }
            throw new JsonSchemaValidationException("Json data is not valid.", StreamSupport.stream(report.spliterator(), false)
                    .map(ProcessingMessage::asJson)
                    .toList());
        } catch (ProcessingException e) {
            throw new JsonSchemaValidationException("Json data validation failed.", e, Collections.singletonList(e.getProcessingMessage().asJson()));    
        }
    }

    private JsonSchemaUtils() {
        throw new UnsupportedOperationException();
    }

}
