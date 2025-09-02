package com.milesight.beaveriot.blueprint.model;

import com.milesight.beaveriot.context.integration.model.BlueprintDevice;
import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/2 14:00
 **/
@Data
public class BlueprintDevices {
    private List<BlueprintDevice> devices;
}
