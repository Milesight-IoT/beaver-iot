package com.milesight.beaveriot.authentication.provider;

import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author loong
 */
@Getter
public class CustomOAuth2RefreshTokenAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String tenantId;
    private final String refreshToken;
    private final Set<String> scopes;

    public CustomOAuth2RefreshTokenAuthenticationToken(String tenantId, String refreshToken, Authentication clientPrincipal,
                                                       @Nullable Set<String> scopes, @Nullable Map<String, Object> additionalParameters) {
        super(AuthorizationGrantType.REFRESH_TOKEN, clientPrincipal, additionalParameters);
        Assert.hasText(refreshToken, "refreshToken cannot be empty");
        this.tenantId = tenantId;
        this.refreshToken = refreshToken;
        this.scopes = Collections.unmodifiableSet((scopes != null) ? new HashSet<>(scopes) : Collections.emptySet());
    }

}
