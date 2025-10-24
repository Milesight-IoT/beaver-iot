package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.google.common.primitives.Longs;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnFindEntityNode;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FnFindEntityExecutor extends AbstractFunctionExecutor<FnFindEntityNode> {

    @Lazy
    @Autowired
    private EntityServiceProvider entityServiceProvider;

    @Override
    public void execute(FnFindEntityNode function, BlueprintDeployContext context) {
        var key = getParameter(function, 0, String.class);
        var id = Longs.tryParse(key);
        if (id != null) {
            setResult(function, entityServiceProvider.findById(id));
        } else {
            setResult(function, entityServiceProvider.findByKey(key));
        }
    }

    @Override
    public Class<FnFindEntityNode> getMatchedNodeType() {
        return FnFindEntityNode.class;
    }

}
