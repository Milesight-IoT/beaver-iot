package com.milesight.beaveriot.context.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceTemplateDetailResponse extends DeviceTemplateResponseData {
    private String identifier;

    private String userNickname;

    private String userEmail;
}
