package com.milesight.beaveriot.blueprint.core.chart.node.data.function;


import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@NoArgsConstructor
public class FnFindEntityNode extends AbstractFunctionNode {

    public FnFindEntityNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public static final String FUNCTION_NAME = "fn::find_entity";

    @Override
    public String getFunctionName() {
        return FUNCTION_NAME;
    }

    @Component
    public static class Parser extends FunctionNode.Parser<FnFindEntityNode> {

        @Override
        public String getFunctionName() {
            return FUNCTION_NAME;
        }

        @Override
        protected FnFindEntityNode createNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
            return new FnFindEntityNode(blueprintNodeParent, blueprintNodeName);
        }
    }

}
