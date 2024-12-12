package com.milesight.beaveriot.rule.model.flow.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.rule.support.JSONHelper;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.List;

/**
 * @author leon
 */
@Data
public class RuleChoiceConfig implements RuleConfig {

    private String id;
    private String componentId;
    private String nodeName;
    private List<RuleChoiceWhenConfig> when;
    private RuleChoiceOtherwiseConfig otherwise;

    @SneakyThrows
    public static RuleChoiceConfig create(JsonNode parameters) {
        return JSONHelper.fromJSON(JSONHelper.toJSON(parameters), RuleChoiceConfig.class);
    }

    @Data
    public static class RuleChoiceWhenConfig implements RuleConfig {
        private String id;
        private String expressionType;
        private List<ExpressionConfig> and;
        private List<ExpressionConfig> or;

        @Override
        public String getComponentId() {
            return COMPONENT_CHOICE_WHEN;
        }

    }

    @Data
    public static class RuleChoiceOtherwiseConfig implements RuleConfig {
        private String id;

        @Override
        public String getComponentId() {
            return COMPONENT_CHOICE_OTHERWISE;
        }
    }

}
