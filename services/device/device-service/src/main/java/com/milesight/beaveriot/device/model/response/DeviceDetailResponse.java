package com.milesight.beaveriot.device.model.response;

import com.milesight.beaveriot.device.dto.DeviceResponseData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceDetailResponse extends DeviceResponseData {
    private String identifier;

    private String templateName;

    private String userNickname;

    private String userEmail;
}
