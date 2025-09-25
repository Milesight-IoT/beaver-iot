package com.milesight.beaveriot.blueprint.core.chart.node.data.function;


import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@NoArgsConstructor
public class FnFormatNode extends AbstractFunctionNode {

    public FnFormatNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public static final String FUNCTION_NAME = "fn::format";

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Component
    public static class Parser extends FunctionNode.Parser<FnFormatNode> {

        @Override
        public String getFunctionName() {
            return FUNCTION_NAME;
        }

        @Override
        protected FnFormatNode createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
            return new FnFormatNode(blueprintNodeParent, blueprintNodeName);
        }
    }

}
