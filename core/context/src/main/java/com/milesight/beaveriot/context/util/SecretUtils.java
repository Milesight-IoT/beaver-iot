package com.milesight.beaveriot.context.util;

import lombok.extern.slf4j.*;
import org.apache.commons.lang3.RandomStringUtils;


@Slf4j
public class SecretUtils {

    private static final String SECRET_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_+=<>?";

    public static String randomSecret(int length) {
        return RandomStringUtils.random(length, SECRET_CHARACTERS);
    }

}
