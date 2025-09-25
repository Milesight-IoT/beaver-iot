package com.milesight.beaveriot.blueprint.core.chart.node.data.function;


import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@NoArgsConstructor
public class FnGetNode extends AbstractFunctionNode {

    public FnGetNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public static final String FUNCTION_NAME = "fn::get";

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Component
    public static class Parser extends FunctionNode.Parser<FnGetNode> {

        @Override
        public String getFunctionName() {
            return FUNCTION_NAME;
        }

        @Override
        protected FnGetNode createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
            return new FnGetNode(blueprintNodeParent, blueprintNodeName);
        }
    }

}
