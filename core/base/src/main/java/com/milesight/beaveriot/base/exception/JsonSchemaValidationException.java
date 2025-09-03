package com.milesight.beaveriot.base.exception;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.List;


public class JsonSchemaValidationException extends Exception {

    @Getter
    private List<JsonNode> details;

    public JsonSchemaValidationException(String message, List<JsonNode> details) {
        super(message);
        this.details = details;
    }

    public JsonSchemaValidationException(Throwable cause, List<JsonNode> details) {
        super(cause);
        this.details = details;
    }

    public JsonSchemaValidationException(String message, Throwable cause, List<JsonNode> details) {
        super(message, cause);
        this.details = details;
    }

}
