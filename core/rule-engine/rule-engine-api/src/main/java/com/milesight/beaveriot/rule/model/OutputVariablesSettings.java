package com.milesight.beaveriot.rule.model;

import com.milesight.beaveriot.rule.enums.DataTypeEnums;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
@Data
public class OutputVariablesSettings {

    private DataTypeEnums type;
    private String name;

    public static void validate(Object result, List<OutputVariablesSettings> outputVariablesSettings) {
        if (ObjectUtils.isEmpty(outputVariablesSettings) || !(result instanceof Map resultMap) ) {
            return;
        }

        for (OutputVariablesSettings config : outputVariablesSettings) {
            String paramName = config.getName();
            DataTypeEnums paramType = config.getType();
            if (resultMap.containsKey(paramName) ) {
                paramType.validate(paramName, resultMap.get(paramName));
            }
        }
    }
}
