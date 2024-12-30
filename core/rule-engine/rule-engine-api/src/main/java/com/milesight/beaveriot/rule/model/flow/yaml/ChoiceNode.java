package com.milesight.beaveriot.rule.model.flow.yaml;

import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.ExpressionAware;
import com.milesight.beaveriot.rule.model.flow.yaml.base.NodeId;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChoiceNode implements OutputNode {

    private InnerChoiceNode choice;

    public static ChoiceNodeBuilder builder() {
        return new ChoiceNodeBuilder();
    }

    @Override
    public String getId() {
        return choice.getId();
    }

    public static class ChoiceNodeBuilder {
        private String id;
        private List<InnerChoiceNode.WhenCaseNode> whenCaseNodeList = new ArrayList<>();

        private InnerChoiceNode.OtherwiseNode otherwiseNode;

        public ChoiceNodeBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ChoiceNodeBuilder when(String id, ExpressionNode expression, List<OutputNode> steps) {
            if (ObjectUtils.isEmpty(steps)) {
                return this;
            }
            whenCaseNodeList.add(new InnerChoiceNode.WhenCaseNode(id, expression, steps));
            return this;
        }

        public ChoiceNodeBuilder otherwise(String id, List<OutputNode> steps) {
            if (ObjectUtils.isEmpty(steps)) {
                return this;
            }
            otherwiseNode = new InnerChoiceNode.OtherwiseNode(id, steps);
            return this;
        }

        public ChoiceNode build() {
            Assert.notNull(whenCaseNodeList, "whenCaseNodeList must not be null");
            return new ChoiceNode(new InnerChoiceNode(id, whenCaseNodeList, otherwiseNode));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InnerChoiceNode implements NodeId {

        private String id;
        private List<InnerChoiceNode.WhenCaseNode> when;
        private InnerChoiceNode.OtherwiseNode otherwise;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class OtherwiseNode implements NodeId {

            private String id;
            private List<OutputNode> steps;

        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class WhenCaseNode implements ExpressionAware, NodeId {

            private String id;
            private ExpressionNode expression;
            private List<OutputNode> steps;

        }
    }
}
