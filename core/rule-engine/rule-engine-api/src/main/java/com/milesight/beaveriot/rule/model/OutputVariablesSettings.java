package com.milesight.beaveriot.rule.model;

import com.milesight.beaveriot.rule.enums.DataTypeEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutputVariablesSettings implements VariableNamed {

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
                resultMap.put(paramName, paramType.validate(paramName, resultMap.get(paramName)));
            }
        }
    }
}
