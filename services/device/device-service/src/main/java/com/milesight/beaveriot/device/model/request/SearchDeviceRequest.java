package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SearchDeviceRequest extends GenericPageRequest {
    @Size(max = DeviceDataFieldConstants.DEVICE_NAME_MAX_LENGTH)
    private String name;
    private String template;
    private String groupId;
    private Boolean filterNotGrouped;
    private String identifier;
    private List<Long> idList = new ArrayList<>();
}
