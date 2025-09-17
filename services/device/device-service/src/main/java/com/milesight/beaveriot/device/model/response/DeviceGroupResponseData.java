package com.milesight.beaveriot.device.model.response;

import lombok.Data;

/**
 * DeviceGroupResponseData class.
 *
 * @author simon
 * @date 2025/6/25
 */
@Data
public class DeviceGroupResponseData {
    private String id;

    private String name;

    private Integer deviceCount;
}
