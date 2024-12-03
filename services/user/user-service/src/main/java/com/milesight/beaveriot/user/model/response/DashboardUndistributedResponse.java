package com.milesight.beaveriot.user.model.response;

import lombok.Data;

/**
 * @author loong
 * @date 2024/12/3 9:26
 */
@Data
public class DashboardUndistributedResponse {

    private String dashboardId;
    private String dashboardName;
    private String createdAt;
    private String userId;
    private String userEmail;
    private String userNickname;

}
