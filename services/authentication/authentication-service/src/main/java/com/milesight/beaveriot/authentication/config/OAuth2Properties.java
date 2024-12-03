package com.milesight.beaveriot.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author loong
 * @date 2024/10/24 14:39
 */
@Component
@Data
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {

    private String registeredClientId;
    private String clientId;
    private String clientSecret;
    private String[] ignoreUrls;
    private RsaKey rsa;

    @Data
    public static class RsaKey {
        private String publicKey;
        private String privateKey;
    }

}
