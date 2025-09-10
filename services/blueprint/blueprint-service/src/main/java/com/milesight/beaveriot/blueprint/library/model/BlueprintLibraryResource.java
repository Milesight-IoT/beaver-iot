package com.milesight.beaveriot.blueprint.library.model;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/1 9:37
 **/
@Builder
@Data
public class BlueprintLibraryResource {
    private String path;
    private String content;
    private Long libraryId;
    private String libraryVersion;
}