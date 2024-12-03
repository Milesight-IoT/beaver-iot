package com.milesight.beaveriot.user.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author loong
 * @date 2024/10/18 9:27
 */
@Slf4j
public class SignUtils {

    public static String sha256Hex(String data) {
        try {
            byte[] msg = data.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return byteArray2HexString(md.digest(msg));
        } catch (Exception e) {
            log.error("execute sha256Hex error:{}", e.getMessage(), e);
        }
        return null;
    }

    public static String byteArray2HexString(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString().toLowerCase();
    }

}
