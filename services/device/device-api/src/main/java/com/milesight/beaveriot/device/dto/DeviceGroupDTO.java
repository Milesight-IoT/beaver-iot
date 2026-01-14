package com.milesight.beaveriot.device.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DeviceGroupDTO class.
 *
 * @author simon
 * @date 2026/1/13
 */
@Data
@Builder
public class DeviceGroupDTO {
    private Long groupId;
    private String groupName;
}
