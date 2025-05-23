package com.milesight.beaveriot.user.constants;

/**
 * UserDataFieldConstants class.
 *
 * @author simon
 * @date 2025/5/21
 */
public class UserDataFieldConstants {
    private UserDataFieldConstants() {}

    public static final int USER_EMAIL_MIN_LENGTH = 5;
    public static final int USER_EMAIL_MAX_LENGTH = 255;
    public static final int USER_NICKNAME_MIN_LENGTH = 1;
    public static final int USER_NICKNAME_MAX_LENGTH = 127;
    public static final int USER_PASSWORD_MIN_LENGTH = 8;
    public static final int USER_PASSWORD_MAX_LENGTH = 63;
    public static final int ROLE_NAME_MIN_LENGTH = 1;
    public static final int ROLE_NAME_MAX_LENGTH = 127;
    public static final int ROLE_DESCRIPTION_MAX_LENGTH = 255;
}
