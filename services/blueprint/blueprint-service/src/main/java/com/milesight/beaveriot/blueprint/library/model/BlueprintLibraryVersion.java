package com.milesight.beaveriot.blueprint.library.model;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/19 10:11
 **/
@Builder
@Data
public class BlueprintLibraryVersion {
    private Long id;
    private Long libraryId;
    private String libraryVersion;
    private Long syncedAt;

    public String getKey() {
        return libraryId + ":" + libraryVersion;
    }
}
