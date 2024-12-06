package com.milesight.beaveriot.user.model.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.base.page.Sorts;
import lombok.Data;

/**
 * @author loong
 * @date 2024/12/3 10:14
 */
@Data
public class UserRolePageRequest extends GenericPageRequest {

    private String keyword;
    @JsonUnwrapped
    protected Sorts sort;
}
