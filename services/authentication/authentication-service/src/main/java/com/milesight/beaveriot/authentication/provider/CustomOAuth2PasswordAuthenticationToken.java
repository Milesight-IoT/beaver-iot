package com.milesight.beaveriot.authentication.provider;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/12 9:24
 */
@Getter
public class CustomOAuth2PasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String tenantId;
    private final String username;
    private final String password;

    public CustomOAuth2PasswordAuthenticationToken(String tenantId, String username, String password, Authentication clientPrincipal, Map<String, Object> additionalParameters) {
        super(AuthorizationGrantType.PASSWORD, clientPrincipal, additionalParameters);
        this.tenantId = tenantId;
        this.username = username;
        this.password = password;
    }

}
