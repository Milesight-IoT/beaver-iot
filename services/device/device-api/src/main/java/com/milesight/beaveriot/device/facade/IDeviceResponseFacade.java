package com.milesight.beaveriot.device.facade;

import com.milesight.beaveriot.device.dto.DeviceResponseData;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * IDeviceResponseFacade
 *
 * @author simon
 * @date 2025/9/18
 */
public interface IDeviceResponseFacade {
    Page<DeviceResponseData> getDeviceResponseByIds(List<Long> deviceIds);
}
