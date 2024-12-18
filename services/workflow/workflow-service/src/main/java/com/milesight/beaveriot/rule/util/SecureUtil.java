package com.milesight.beaveriot.rule.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @author loong
 * @date 2024/12/20 12:43
 */
@Slf4j
public class SecureUtil {

    public static String hmacSha256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), mac.getAlgorithm());
            mac.init(signingKey);
            byte[] signData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return byteArray2HexString(signData);
        } catch (Exception e) {
            log.error("execute hmacSha256Hex error:{}", e.getMessage(), e);
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
