package com.milesight.beaveriot.rule.model.flow.route;

import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.config.RuleChoiceConfig;
import lombok.Data;
import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author leon
 */
@Data
public class ChoiceNodeDefinition extends AbstractNodeDefinition {

    private Map<String, WhenNodeDefinition> whenNodeDefinitions = new LinkedHashMap<>();
    private String otherwiseNodeId;

    public static ChoiceNodeDefinition create(RuleChoiceConfig ruleChoiceConfig) {
        //choice edge and nodes init
        Assert.notNull(ruleChoiceConfig, "Invalid choice config, parameters is null");

        ChoiceNodeDefinition choiceNodeDefinition = new ChoiceNodeDefinition();

        ruleChoiceConfig.getWhen().forEach(when -> {
            WhenNodeDefinition whenNodeDefinition = new WhenNodeDefinition(ExpressionNode.create(when));
            choiceNodeDefinition.getWhenNodeDefinitions().put(when.getId(), whenNodeDefinition);
        });

        RuleChoiceConfig.RuleChoiceOtherwiseConfig otherwise = ruleChoiceConfig.getOtherwise();
        if (otherwise != null) {
            choiceNodeDefinition.setOtherwiseNodeId(otherwise.getId());
        }
        choiceNodeDefinition.setId(ruleChoiceConfig.getId());
        choiceNodeDefinition.setNameNode(ruleChoiceConfig.getName());
        return choiceNodeDefinition;
    }
}
