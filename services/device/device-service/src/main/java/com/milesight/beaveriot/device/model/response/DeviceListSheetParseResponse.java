package com.milesight.beaveriot.device.model.response;

import com.milesight.beaveriot.device.model.request.CreateDeviceRequest;
import lombok.Data;

import java.util.List;

/**
 * DeviceListSheetParseResponse class.
 *
 * @author simon
 * @date 2025/7/4
 */
@Data
public class DeviceListSheetParseResponse {
    List<CreateDeviceRequest> createDeviceRequests;

    List<Integer> rowId;
}
