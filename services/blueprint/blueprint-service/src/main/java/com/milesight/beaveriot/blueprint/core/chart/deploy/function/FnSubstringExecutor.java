package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnSubstringNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FnSubstringExecutor extends AbstractFunctionExecutor<FnSubstringNode> {
    @Override
    public void execute(FnSubstringNode function, BlueprintDeployContext context) {
        var str = getParameter(function, 0, String.class);

        var start = getParameter(function, 1, Integer.class);
        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            start = str.length();
        }

        var end = getParameter(function, 2, Integer.class, false);
        if (end == null) {
            setResult(function, str.substring(start));
        } else {
            if (end > str.length()) {
                end = str.length();
            }
            setResult(function, str.substring(start, end));
        }
    }

    @Override
    public Class<FnSubstringNode> getMatchedNodeType() {
        return FnSubstringNode.class;
    }

}
