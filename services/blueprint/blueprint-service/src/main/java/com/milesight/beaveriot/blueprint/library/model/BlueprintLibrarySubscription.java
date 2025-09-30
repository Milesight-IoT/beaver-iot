package com.milesight.beaveriot.blueprint.library.model;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/19 10:12
 **/
@Builder
@Data
public class BlueprintLibrarySubscription {
    private Long id;
    private Long libraryId;
    private String libraryVersion;
    private Boolean active;
    private String tenantId;

    public String getKey() {
        return libraryId + ":" + libraryVersion;
    }
}
