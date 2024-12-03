package com.milesight.beaveriot.user.model.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.milesight.beaveriot.base.page.Sorts;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/22 11:51
 */
@Data
public class UserUndistributedRequest {

    private String keyword;
    @JsonUnwrapped
    protected Sorts sort;

}
