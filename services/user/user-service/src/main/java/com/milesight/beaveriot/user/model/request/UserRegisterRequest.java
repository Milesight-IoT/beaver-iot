package com.milesight.beaveriot.user.model.request;

import lombok.Data;

/**
 * @author loong
 * @date 2024/10/14 11:23
 */
@Data
public class UserRegisterRequest {

    private String email;
    private String nickname;
    private String password;

}
