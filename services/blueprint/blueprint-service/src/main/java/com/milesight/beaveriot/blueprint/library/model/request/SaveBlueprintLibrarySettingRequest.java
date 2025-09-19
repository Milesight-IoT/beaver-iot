package com.milesight.beaveriot.blueprint.library.model.request;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/17 17:21
 **/
@Data
public class SaveBlueprintLibrarySettingRequest {
    private String sourceType;
    private String type;
    private String url;
    private String branch = "main";
}
