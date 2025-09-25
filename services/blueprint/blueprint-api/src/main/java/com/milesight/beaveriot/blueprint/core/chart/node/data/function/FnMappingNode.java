package com.milesight.beaveriot.blueprint.core.chart.node.data.function;


import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@NoArgsConstructor
public class FnMappingNode extends AbstractFunctionNode {

    public FnMappingNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public static final String FUNCTION_NAME = "fn::mapping";

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Component
    public static class Parser extends FunctionNode.Parser<FnMappingNode> {

        @Override
        public String getFunctionName() {
            return FUNCTION_NAME;
        }

        @Override
        protected FnMappingNode createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
            return new FnMappingNode(blueprintNodeParent, blueprintNodeName);
        }
    }

}
