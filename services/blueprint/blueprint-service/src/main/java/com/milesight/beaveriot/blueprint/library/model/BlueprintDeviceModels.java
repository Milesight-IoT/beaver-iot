package com.milesight.beaveriot.blueprint.library.model;

import com.milesight.beaveriot.context.integration.model.BlueprintDeviceModel;
import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 14:00
 **/
@Data
public class BlueprintDeviceModels {
    private List<BlueprintDeviceModel> models;
}
