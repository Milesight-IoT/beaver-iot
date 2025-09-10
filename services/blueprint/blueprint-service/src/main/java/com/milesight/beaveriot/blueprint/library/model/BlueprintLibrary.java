package com.milesight.beaveriot.blueprint.library.model;

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
public class BlueprintLibrary {
    private Long id;
    private String home;
    private String branch;
    private String currentVersion;
    private String remoteVersion;
    private BlueprintLibrarySyncStatus syncStatus;
    private Long syncedAt;

    public static BlueprintLibrary clone(BlueprintLibrary blueprintLibrary) {
        return BlueprintLibrary.builder()
                .id(blueprintLibrary.getId())
                .home(blueprintLibrary.getHome())
                .branch(blueprintLibrary.getBranch())
                .currentVersion(blueprintLibrary.getCurrentVersion())
                .remoteVersion(blueprintLibrary.getRemoteVersion())
                .syncStatus(blueprintLibrary.getSyncStatus())
                .syncedAt(blueprintLibrary.getSyncedAt()).build();
    }
}
