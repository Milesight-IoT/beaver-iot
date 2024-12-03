package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;

/**
 * @author loong
 * @date 2024/12/3 9:51
 */
@Data
public class RoleIntegrationRequest extends GenericPageRequest {

    private String keyword;
}
