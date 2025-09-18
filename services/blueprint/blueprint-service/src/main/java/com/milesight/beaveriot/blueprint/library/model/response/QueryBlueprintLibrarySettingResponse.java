package com.milesight.beaveriot.blueprint.library.model.response;

import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/17 15:08
 **/
@Data
public class QueryBlueprintLibrarySettingResponse {
    private String currentSourceType;
    private Map<String, List<BlueprintLibraryAddress>> blueprintLibraryAddresses;

    public static QueryBlueprintLibrarySettingResponse of(String currentSwitchType, Map<String, List<BlueprintLibraryAddress>> blueprintLibraryAddresses) {
        QueryBlueprintLibrarySettingResponse response = new QueryBlueprintLibrarySettingResponse();
        response.setCurrentSourceType(currentSwitchType);
        response.setBlueprintLibraryAddresses(blueprintLibraryAddresses);
        return response;
    }
}