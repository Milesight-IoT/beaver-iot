package com.milesight.beaveriot.authentication.provider;

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

/**
 * @author loong
 * @date 2024/10/29 10:21
 */
public interface CustomOAuth2AuthorizationService extends OAuth2AuthorizationService {

    void removeByPrincipalName(String principalName);

}
