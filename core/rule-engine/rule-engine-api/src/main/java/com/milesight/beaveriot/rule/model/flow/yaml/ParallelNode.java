package com.milesight.beaveriot.rule.model.flow.yaml;

import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.MultiOutputNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParallelNode implements MultiOutputNode {

    private SplitNode splitNode;

    private ChoiceNode choiceNode;

    public static ParallelBuilder builder() {
        return new ParallelBuilder();
    }

    @Override
    public List<OutputNode> getOutputNodes() {
        return List.of(splitNode, choiceNode);
    }

    @Override
    public String getId() {
        return choiceNode.getId();
    }

    public static class ParallelBuilder {

        private String id;
        private AtomicInteger caseIdx = new AtomicInteger(0);
        private List<ChoiceNode.InnerChoiceNode.WhenCaseNode> whenCaseNodeList = new ArrayList<>();

        public ParallelBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ParallelBuilder then(String branchId, List<OutputNode> steps) {
            if (ObjectUtils.isEmpty(steps)) {
                return this;
            }
            whenCaseNodeList.add(new ChoiceNode.InnerChoiceNode.WhenCaseNode(branchId, ExpressionNode.create("simple", "${header.CamelSplitIndex} == " + caseIdx.getAndIncrement()), steps));
            return this;
        }

        public ParallelBuilder then(List<OutputNode> steps) {
            return then(null, steps);
        }

        public ParallelNode build() {
            Assert.notNull(whenCaseNodeList, "whenCaseNodeList must not be null");
            ChoiceNode choiceNode = new ChoiceNode(new ChoiceNode.InnerChoiceNode(id, whenCaseNodeList, null));
            SplitNode splitNode = SplitNode.create(ExpressionNode.create("simple", "${bean:" + RuleNodeNames.innerParallelSplitter + ".split(${body},'" + whenCaseNodeList.size() + "')}"), true);
            return new ParallelNode(splitNode, choiceNode);
        }

    }
}
