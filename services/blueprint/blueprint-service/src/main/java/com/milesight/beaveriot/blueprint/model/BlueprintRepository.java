package com.milesight.beaveriot.blueprint.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * author: Luxb
 * create: 2025/9/1 14:42
 **/
@Builder
@Jacksonized
@Data
public class BlueprintRepository {
    private Long id;
    private String home;
    private String branch;
    private String currentVersion;
    private String remoteVersion;
    private BlueprintRepositorySyncStatus syncStatus;
    private Long syncedAt;
}
