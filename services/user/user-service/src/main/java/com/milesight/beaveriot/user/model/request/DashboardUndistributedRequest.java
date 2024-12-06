package com.milesight.beaveriot.user.model.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.milesight.beaveriot.base.page.Sorts;
import lombok.Data;

/**
 * @author loong
 * @date 2024/12/3 9:45
 */
@Data
public class DashboardUndistributedRequest {

    private String keyword;
    @JsonUnwrapped
    protected Sorts sort;
}
