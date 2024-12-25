package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/22 11:51
 */
@Data
public class UserUndistributedRequest extends GenericPageRequest {

    private String keyword;

}
