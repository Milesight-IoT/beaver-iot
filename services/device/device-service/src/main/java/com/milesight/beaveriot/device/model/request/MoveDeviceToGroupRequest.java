package com.milesight.beaveriot.device.model.request;

import lombok.Data;

import java.util.List;

/**
 * MoveDeviceToGroupRequest class.
 *
 * @author simon
 * @date 2025/7/3
 */
@Data
public class MoveDeviceToGroupRequest {
    private String groupId;
    private List<String> deviceIdList;
}
