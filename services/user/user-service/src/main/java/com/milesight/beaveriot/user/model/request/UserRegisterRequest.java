package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.user.constants.UserDataFieldConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author loong
 * @date 2024/10/14 11:23
 */
@Data
public class UserRegisterRequest {

    @Email
    @NotBlank
    @Size(min = UserDataFieldConstants.USER_EMAIL_MIN_LENGTH, max = UserDataFieldConstants.USER_EMAIL_MAX_LENGTH)
    private String email;

    @NotBlank
    @Size(min = UserDataFieldConstants.USER_NICKNAME_MIN_LENGTH, max = UserDataFieldConstants.USER_NICKNAME_MAX_LENGTH)
    private String nickname;

    @NotBlank
    @Size(min = UserDataFieldConstants.USER_PASSWORD_MIN_LENGTH, max = UserDataFieldConstants.USER_PASSWORD_MAX_LENGTH)
    private String password;

}
