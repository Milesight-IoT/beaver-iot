package com.milesight.beaveriot.user.dto;

import lombok.Data;

/**
 * @author loong
 * @date 2024/12/5 10:51
 */
@Data
public class TenantDTO {

    private String tenantId;

    private String tenantName;

    private String tenantDomain;

    private String timeZone;
}
