package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;

/**
 * @author loong
 * @date 2024/12/3 10:14
 */
@Data
public class UserRolePageRequest extends GenericPageRequest {

    private String keyword;
}
