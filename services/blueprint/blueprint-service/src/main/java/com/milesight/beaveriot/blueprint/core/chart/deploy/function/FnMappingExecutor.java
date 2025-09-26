package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnMappingNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
public class FnMappingExecutor extends AbstractFunctionExecutor<FnMappingNode> {

    @Override
    public void execute(FnMappingNode function, BlueprintDeployContext context) {
        var elements = function.getParameters();
        var result = new HashMap<String, Object>();
        elements.forEach(v -> {
            if (v.getValue() instanceof List<?> pair && pair.size() > 1) {
                var key = BlueprintUtils.convertValue(pair.get(0), String.class);
                var value = pair.get(1);
                if (key != null && value != null) {
                    result.put(key, value);
                }
            }
        });
        setResult(function, result);
    }

    @Override
    public Class<FnMappingNode> getMatchedNodeType() {
        return FnMappingNode.class;
    }

}
