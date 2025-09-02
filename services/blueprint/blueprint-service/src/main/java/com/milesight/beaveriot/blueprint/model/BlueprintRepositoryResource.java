package com.milesight.beaveriot.blueprint.model;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/1 9:37
 **/
@Builder
@Data
public class BlueprintRepositoryResource {
    private String path;
    private String content;
    private Long repositoryId;
    private String repositoryVersion;
}