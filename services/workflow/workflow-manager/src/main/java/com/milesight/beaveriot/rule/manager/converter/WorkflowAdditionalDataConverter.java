package com.milesight.beaveriot.rule.manager.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.rule.manager.model.WorkflowAdditionalData;
import jakarta.persistence.AttributeConverter;

/**
 * WorkflowAdditionalDataConverter class.
 *
 * @author simon
 * @date 2025/9/23
 */
public class WorkflowAdditionalDataConverter implements AttributeConverter<WorkflowAdditionalData, String> {
    public String convertToDatabaseColumn(WorkflowAdditionalData additionalData) {
        return JsonUtils.toJSON(additionalData);
    }

    @Override
    public WorkflowAdditionalData convertToEntityAttribute(String dbData) {
        return JsonUtils.fromJSON(dbData, WorkflowAdditionalData.class);
    }
}
