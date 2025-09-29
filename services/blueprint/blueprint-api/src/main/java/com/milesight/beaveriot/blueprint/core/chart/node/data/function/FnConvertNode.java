package com.milesight.beaveriot.blueprint.core.chart.node.data.function;


import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@NoArgsConstructor
public class FnConvertNode extends AbstractFunctionNode {

    public FnConvertNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public static final String FUNCTION_NAME = "fn::convert";

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Component
    public static class Parser extends FunctionNode.Parser<FnConvertNode> {

        @Override
        public String getFunctionName() {
            return FUNCTION_NAME;
        }

        @Override
        protected FnConvertNode createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
            return new FnConvertNode(blueprintNodeParent, blueprintNodeName);
        }
    }

}
