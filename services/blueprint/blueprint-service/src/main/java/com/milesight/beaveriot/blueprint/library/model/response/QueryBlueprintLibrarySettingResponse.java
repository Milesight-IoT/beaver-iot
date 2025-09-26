package com.milesight.beaveriot.blueprint.library.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/17 15:08
 **/
@Data
public class QueryBlueprintLibrarySettingResponse {
    private String currentSourceType;
    private String type;
    private String url;
    private String fileName;
    private String version;
    private Long updateTime;
    private boolean syncedSuccess;
}