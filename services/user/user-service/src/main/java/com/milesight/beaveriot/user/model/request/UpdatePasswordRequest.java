package com.milesight.beaveriot.user.model.request;

import lombok.Data;

/**
 * @author loong
 * @date 2024/11/25 13:08
 */
@Data
public class UpdatePasswordRequest {

    private String oldPassword;
    private String newPassword;

}
