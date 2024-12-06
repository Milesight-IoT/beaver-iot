package com.milesight.beaveriot.user.model.response;

import lombok.Data;

/**
 * @author loong
 * @date 2024/12/2 18:02
 */
@Data
public class DeviceUndistributedResponse {

    private String deviceId;
    private String deviceName;
    private String createdAt;
    private String integrationId;
    private String integrationName;
    private String userId;
    private String userEmail;
    private String userNickname;

}
