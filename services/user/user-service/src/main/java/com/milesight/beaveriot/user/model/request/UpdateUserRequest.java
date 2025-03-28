package com.milesight.beaveriot.user.model.request;

import lombok.Data;

/**
 * @author loong
 * @date 2024/11/20 11:10
 */
@Data
public class UpdateUserRequest {

    private String email;
    private String nickname;

}
