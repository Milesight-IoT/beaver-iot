package com.milesight.beaveriot.user.model.response;

import lombok.Data;

/**
 * @author loong
 * @date 2024/12/2 18:02
 */
@Data
public class RoleIntegrationResponse {

    private String integrationId;
    private String integrationName;
    private Long deviceNum;
    private Long entityNum;

}
