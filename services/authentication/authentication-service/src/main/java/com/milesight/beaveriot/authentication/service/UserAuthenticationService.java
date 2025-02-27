package com.milesight.beaveriot.authentication.service;

import com.milesight.beaveriot.authentication.config.OAuth2Properties;
import com.milesight.beaveriot.authentication.provider.CustomOAuth2AuthorizationService;
import com.milesight.beaveriot.authentication.util.OAuth2EndpointUtils;
import com.milesight.beaveriot.context.security.SecurityUser;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

/**
 * @author loong
 * @date 2024/10/14 11:37
 */
@Service
public class UserAuthenticationService {

    @Autowired
    CustomOAuth2AuthorizationService authorizationService;
    @Autowired
    JwtDecoder jwtDecoder;
    @Autowired
    OAuth2Properties oAuth2Properties;

    public void loadSecurityContext(HttpServletRequest request) {
        String authorizationValue = request.getHeader("Authorization");
        boolean isAuthorization = !OAuth2EndpointUtils.getWhiteListMatcher(oAuth2Properties.getIgnoreUrls()).matches(request);
        if (isAuthorization && authorizationValue == null) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "Missing access token", null);
        }
        if (authorizationValue != null) {
            if (!authorizationValue.startsWith("Bearer ")) {
                if(isAuthorization) {
                    OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid access token", null);
                }
            }else {
                String token = authorizationValue.substring(7);
                try {
                    Jwt jwt = readAccessToken(token);
                    SecurityUser securityUser = SecurityUser.create(jwt.getClaims());
                    SecurityUserContext.setSecurityUser(securityUser);
                } catch (Exception e) {
                    if(isAuthorization) {
                        OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid access token", null);
                    }
                }
            }
        }
    }

    public Jwt readAccessToken(String accessToken) {
        OAuth2Authorization authorization = authorizationService.findByToken(accessToken, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null || authorization.getAccessToken() == null || authorization.getAccessToken().isExpired()) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid access token", null);
        }
        return jwtDecoder.decode(accessToken);
    }

}
