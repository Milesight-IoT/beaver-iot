package com.milesight.beaveriot.rule.support;

import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.yaml.*;
import com.milesight.beaveriot.rule.model.flow.yaml.base.ExpressionAware;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.List;

/**
 * @author leon
 */
public class RuleFlowYamlDumper {

    private DumperOptions options;
    private RuleNodeRepresenter ruleNodeRepresenter;

    public RuleFlowYamlDumper() {
        options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        ruleNodeRepresenter = new RuleNodeRepresenter(options);
    }

    public String dump(RouteNode routeNode) {
        Yaml yaml = new Yaml(ruleNodeRepresenter, options);
        return yaml.dump(List.of(routeNode));
    }

    public static class RuleNodeRepresenter extends Representer {
        public RuleNodeRepresenter(DumperOptions options) {
            super(options);
            getPropertyUtils().setSkipMissingProperties(true);
            addClassTag(RouteNode.class, Tag.MAP);
            addClassTag(RuleNode.class, Tag.MAP);
            addClassTag(SplitNode.class, Tag.MAP);
            addClassTag(ChoiceNode.class, Tag.MAP);
            addClassTag(OutputNode.class, Tag.MAP);
            addClassTag(ParallelNode.class, Tag.MAP);
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
            if (propertyValue == null) {
                return null;
            }

            if (javaBean instanceof ExpressionAware expressionAware && property.getName().equals(expressionAware.getExpressionProperty())) {
                ExpressionNode expression = expressionAware.getExpression();
                if (expression == null || !expression.validate()) {
                    throw new IllegalArgumentException("Expression is invalid: " + property);
                }
                return new NodeTuple(new ScalarNode(Tag.STR, expressionAware.getExpression().getLanguage(), null, null, DumperOptions.ScalarStyle.PLAIN),
                        new ScalarNode(Tag.STR, expressionAware.getExpression().getExpression(), null, null, DumperOptions.ScalarStyle.DOUBLE_QUOTED));
            }
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }

}
