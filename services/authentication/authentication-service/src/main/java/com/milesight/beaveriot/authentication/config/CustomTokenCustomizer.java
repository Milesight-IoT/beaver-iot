package com.milesight.beaveriot.authentication.config;

import com.milesight.beaveriot.authentication.util.OAuth2EndpointUtils;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * @author loong
 * @date 2024/10/12 9:35
 */
@Component
public class CustomTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Autowired
    IUserFacade userFacade;

    @Override
    public void customize(JwtEncodingContext context) {
        String username = context.getPrincipal().getName();
        UserDTO userDTO = userFacade.getEnableUserByEmail(username);
        if (userDTO == null) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "user not found.", null);
        }
        context.getClaims().claims(claims -> {
            claims.put(TenantContext.TENANT_ID, userDTO.getTenantId());
            claims.put(SecurityUserContext.USER_ID, userDTO.getUserId());
            claims.put("nickname", userDTO.getNickname());
            claims.put("email", userDTO.getEmail());
            claims.put("createdAt", userDTO.getCreatedAt());
        });
    }

}
