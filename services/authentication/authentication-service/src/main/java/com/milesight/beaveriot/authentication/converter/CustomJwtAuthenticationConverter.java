package com.milesight.beaveriot.authentication.converter;

import com.milesight.beaveriot.authentication.provider.CustomOAuth2AuthorizationService;
import com.milesight.beaveriot.authentication.util.OAuth2EndpointUtils;
import com.milesight.beaveriot.context.security.SecurityUser;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Map;

/**
 * @author loong
 */
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private String principalClaimName = JwtClaimNames.SUB;

    private final CustomOAuth2AuthorizationService authorizationService;

    public CustomJwtAuthenticationConverter(CustomOAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String accessToken = jwt.getTokenValue();

        OAuth2Authorization authorization = authorizationService.findByToken(accessToken, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid access token", null);
        }

        Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);

        Map<String, Object> claims = jwt.getClaims();
        SecurityUser securityUser = SecurityUser.create(claims);
        SecurityUserContext.setSecurityUser(securityUser);

        String principalClaimValue = jwt.getClaimAsString(this.principalClaimName);
        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }
}
