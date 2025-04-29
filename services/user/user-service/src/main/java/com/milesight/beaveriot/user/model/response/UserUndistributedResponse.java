package com.milesight.beaveriot.user.model.response;

import lombok.Data;

/**
 * @author loong
 * @date 2024/10/21 17:04
 */
@Data
public class UserUndistributedResponse {

    private String userId;
    private String nickname;
    private String email;
    private String createdAt;

}
