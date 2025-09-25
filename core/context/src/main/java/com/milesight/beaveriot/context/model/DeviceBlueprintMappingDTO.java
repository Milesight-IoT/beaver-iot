package com.milesight.beaveriot.context.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DeviceBlueprintMappingDTO class.
 *
 * @author simon
 * @date 2025/9/24
 */
@Data
@AllArgsConstructor
public class DeviceBlueprintMappingDTO {
    private Long deviceId;

    private Long blueprintId;
}
