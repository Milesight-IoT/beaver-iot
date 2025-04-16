package com.milesight.beaveriot.resource.adapter.db.service.model;

/**
 * DbResourceBasicProjection
 *
 * @author simon
 * @date 2025/4/7
 */
public interface DbResourceBasicProjection {
    String getObjKey();

    String getContentType();

    Long getContentLength();

    Long getCreatedAt();
}
