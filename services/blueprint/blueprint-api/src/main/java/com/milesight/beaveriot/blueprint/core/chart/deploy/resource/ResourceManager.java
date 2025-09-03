package com.milesight.beaveriot.blueprint.core.chart.deploy.resource;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.ResourceNode;
import com.milesight.beaveriot.blueprint.core.model.BindResource;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface ResourceManager<R extends ResourceNode> {

    Class<R> getMatchedNodeType();

    @NotNull
    List<BindResource> deploy(R resource, BlueprintDeployContext context);

    boolean deleteResource(R resource, ResourceMatcher condition);

}
