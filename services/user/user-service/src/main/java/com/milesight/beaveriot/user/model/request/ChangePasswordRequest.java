package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.user.constants.UserDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author loong
 * @date 2024/12/2 13:34
 */
@Data
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = UserDataFieldConstants.USER_PASSWORD_MIN_LENGTH, max = UserDataFieldConstants.USER_PASSWORD_MAX_LENGTH)
    private String password;

}
