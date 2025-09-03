package com.milesight.beaveriot.blueprint.core.chart.deploy.resource;

@FunctionalInterface
public interface ResourceMatcher {

    boolean isMatch(String resourceType, String id);

}
