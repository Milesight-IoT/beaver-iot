package com.milesight.beaveriot.rule.model.flow.config;

import com.milesight.beaveriot.rule.enums.LogicOperator;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Optional;

/**
 * @author leon
 */
@Data
public class RuleChoiceConfig implements RuleConfig {

    private String id;
    private String componentName;
    private RuleChoiceDataConfig data;

    @SneakyThrows
    public static RuleChoiceConfig create(RuleNodeConfig ruleNodeConfig) {
        RuleChoiceConfig ruleChoiceConfig = new RuleChoiceConfig();
        RuleChoiceDataConfig dataConfig = JsonHelper.fromJSON(JsonHelper.toJSON(ruleNodeConfig.getParameters()), RuleChoiceDataConfig.class);
        ruleChoiceConfig.setData(dataConfig);
        ruleChoiceConfig.setId(ruleNodeConfig.getId());
        ruleChoiceConfig.setComponentName(ruleNodeConfig.getComponentName());
        return ruleChoiceConfig;
    }

    @Override
    public String getName() {
        return getConfigDataOptional().orElse(null).getNodeName();
    }

    private Optional<RuleChoiceDataConfig> getConfigDataOptional() {
        RuleChoiceDataConfig dataConfig = getData();
        return dataConfig == null ? Optional.empty() : Optional.of(dataConfig);
    }

    public List<RuleChoiceWhenConfig> getWhen() {
        Optional<RuleChoiceDataConfig> dataConfig = getConfigDataOptional();
        ChoiceSettingConfig choice = dataConfig.orElse(null).getChoice();
        return choice != null ? choice.getWhen() : null;
    }

    public RuleChoiceOtherwiseConfig getOtherwise() {
        Optional<RuleChoiceDataConfig> dataConfig = getConfigDataOptional();
        ChoiceSettingConfig choice = dataConfig.orElse(null).getChoice();
        return choice != null ? choice.getOtherwise() : null;
    }

    @Data
    public static class RuleChoiceDataConfig {
        private String nodeName;
        private ChoiceSettingConfig choice;
    }

    @Data
    public static class ChoiceSettingConfig {
        private List<RuleChoiceWhenConfig> when;
        private RuleChoiceOtherwiseConfig otherwise;
    }

    @Data
    public static class RuleChoiceWhenConfig implements RuleConfig {
        private String id;
        private String expressionType;
        private LogicOperator logicOperator;
        private List<ExpressionConfig> conditions;

        @Override
        public String getComponentName() {
            return COMPONENT_CHOICE_WHEN;
        }

    }

    @Data
    public static class RuleChoiceOtherwiseConfig implements RuleConfig {
        private String id;

        @Override
        public String getComponentName() {
            return COMPONENT_CHOICE_OTHERWISE;
        }
    }

}
