package com.milesight.beaveriot.context.model;

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
    private BlueprintLibraryType type;
    private String url;
    private String branch;
    private String currentVersion;
    private String remoteVersion;
    private BlueprintLibrarySourceType sourceType;
    private BlueprintLibrarySyncStatus syncStatus;
    private Long syncedAt;
    private String syncMessage;

    public static BlueprintLibrary clone(BlueprintLibrary blueprintLibrary) {
        return BlueprintLibrary.builder()
                .id(blueprintLibrary.getId())
                .type(blueprintLibrary.getType())
                .url(blueprintLibrary.getUrl())
                .branch(blueprintLibrary.getBranch())
                .currentVersion(blueprintLibrary.getCurrentVersion())
                .remoteVersion(blueprintLibrary.getRemoteVersion())
                .sourceType(blueprintLibrary.getSourceType())
                .syncStatus(blueprintLibrary.getSyncStatus())
                .syncedAt(blueprintLibrary.getSyncedAt())
                .syncMessage(blueprintLibrary.getSyncMessage()).build();
    }
}
