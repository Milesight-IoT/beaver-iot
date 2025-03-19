package com.milesight.beaveriot.authentication.provider;

import com.milesight.beaveriot.authentication.util.OAuth2EndpointUtils;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.user.constants.UserConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author loong
 * @date 2024/10/12 9:12
 */
public class CustomOAuth2PasswordAuthenticationConverter implements AuthenticationConverter {
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!AuthorizationGrantType.PASSWORD.getValue().equals(grantType)) {
            return null;
        }
        String tenantId = request.getHeader(TenantContext.HEADER_TENANT_ID) != null ? request.getHeader(TenantContext.HEADER_TENANT_ID) : null;
        //FIXME
        if (!StringUtils.hasText(tenantId)) {
            tenantId = UserConstants.DEFAULT_TENANT_ID;
        }
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);
        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
        if (!StringUtils.hasText(username) || parameters.get(OAuth2ParameterNames.USERNAME).size() != 1) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "username is null.", null);
        }
        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
        if (!StringUtils.hasText(password) || parameters.get(OAuth2ParameterNames.PASSWORD).size() != 1) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "password is null.", null);
        }
        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE)
                    && !key.equals(OAuth2ParameterNames.CLIENT_ID)
                    && !key.equals(OAuth2ParameterNames.USERNAME)
                    && !key.equals(OAuth2ParameterNames.PASSWORD)) {
                additionalParameters.put(key, value.get(0));
            }
        });
        return new CustomOAuth2PasswordAuthenticationToken(tenantId, username, password, clientPrincipal, additionalParameters);
    }
}
