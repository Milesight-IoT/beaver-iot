package com.milesight.beaveriot.rule.model.flow.yaml;

import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.ExpressionAware;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SplitNode implements OutputNode {

    private InnerSplitNode split;

    public static SplitNode create(ExpressionNode expression) {
        return new SplitNode(new InnerSplitNode(null, expression, null));
    }

    public static SplitNode create(ExpressionNode expression, boolean parallelProcessing) {
        return new SplitNode(new InnerSplitNode(null, expression, parallelProcessing));
    }

    public static SplitNode create(String id, ExpressionNode expression, boolean parallelProcessing) {
        return new SplitNode(new InnerSplitNode(id, expression, parallelProcessing));
    }

    @Override
    public String getId() {
        return split.getId();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InnerSplitNode implements ExpressionAware {
        private String id;
        private ExpressionNode expression;
        private Boolean parallelProcessing;
    }

}
