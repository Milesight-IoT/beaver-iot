package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;

/**
 * @author loong
 * @date 2024/12/2 13:44
 */
@Data
public class RoleListRequest extends GenericPageRequest {

    private String keyword;
}
