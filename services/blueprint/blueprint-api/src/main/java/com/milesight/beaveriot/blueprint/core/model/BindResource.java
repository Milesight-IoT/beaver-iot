package com.milesight.beaveriot.blueprint.core.model;

public record BindResource(String resourceType, String id, boolean managed) {

    public String getKey() {
        return resourceType + "::" + id;
    }

}
