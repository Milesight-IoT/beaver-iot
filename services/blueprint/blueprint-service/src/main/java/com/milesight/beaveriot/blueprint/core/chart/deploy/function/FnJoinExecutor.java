package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnJoinNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FnJoinExecutor extends AbstractFunctionExecutor<FnJoinNode> {
    @Override
    public void execute(FnJoinNode function, BlueprintDeployContext context) {
        var delimiter = getParameter(function, 0, String.class);
        var elements = getParameter(function, 1, List.class);

        var result = new StringBuilder();
        var lastElementIndex = elements.size() - 1;
        for (int i = 0; i < lastElementIndex; i++) {
            var element = elements.get(i);
            result.append(element).append(delimiter);
        }
        result.append(elements.get(lastElementIndex));

        setResult(function, result.toString());
    }

    @Override
    public Class<FnJoinNode> getMatchedNodeType() {
        return FnJoinNode.class;
    }

}
