package com.milesight.beaveriot.user.model.request;

import com.milesight.beaveriot.user.constants.UserDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author loong
 * @date 2024/11/20 10:10
 */
@Data
public class UpdateRoleRequest {
    @NotBlank
    @Size(min = UserDataFieldConstants.ROLE_NAME_MIN_LENGTH, max = UserDataFieldConstants.ROLE_NAME_MAX_LENGTH)
    private String name;

    @Size(max = UserDataFieldConstants.ROLE_DESCRIPTION_MAX_LENGTH)
    private String description;

}
