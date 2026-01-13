package com.milesight.beaveriot.device.facade;

import com.milesight.beaveriot.device.dto.DeviceGroupDTO;

import java.util.List;
import java.util.Map;

/**
 * IDeviceGroupFacade
 *
 * @author simon
 * @date 2026/1/13
 */
public interface IDeviceGroupFacade {
    Map<Long, DeviceGroupDTO> getGroupFromDeviceId(List<Long> deviceIdList);
}
