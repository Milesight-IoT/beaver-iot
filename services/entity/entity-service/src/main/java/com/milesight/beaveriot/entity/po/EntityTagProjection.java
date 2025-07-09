package com.milesight.beaveriot.entity.po;

public interface EntityTagProjection {
    Long getId();
    String getName();
    String getDescription();
    String getColor();
    Long getCreatedAt();
    Long getUpdatedAt();
    Long getTaggedEntitiesCount();
}
