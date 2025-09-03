package com.milesight.beaveriot.blueprint.core.chart.deploy;

import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface NodeDependencyDiscoverer<T extends BlueprintNode> {

    Class<T> getMatchedNodeType();

    @NotNull
    List<BlueprintNode> discoverDependencies(T blueprintNode, BlueprintDeployContext context);

}
