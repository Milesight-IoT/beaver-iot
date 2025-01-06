package com.milesight.beaveriot.rule.model.flow.yaml;

import com.milesight.beaveriot.rule.model.flow.yaml.base.NodeId;
import com.milesight.beaveriot.rule.support.RuleFlowYamlDumper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteNode implements NodeId {

    private InnerRouteNode route;

    public static RouteNode create(String id, FromNode from) {
        return new RouteNode(new InnerRouteNode(id, from));
    }

    @Override
    public String getId() {
        return route.getId();
    }

    public String dumpYaml() {
        return new RuleFlowYamlDumper().dump(this);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InnerRouteNode implements NodeId {

        private String id;
        private FromNode from;

    }

}
