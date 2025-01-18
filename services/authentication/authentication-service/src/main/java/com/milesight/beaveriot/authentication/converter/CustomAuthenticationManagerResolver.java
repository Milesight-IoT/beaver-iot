package com.milesight.beaveriot.authentication.converter;

import com.milesight.beaveriot.authentication.provider.CustomOAuth2AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

/**
 * @author loong
 */
public class CustomAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final CustomOAuth2AuthorizationService authorizationService;
    private final JwtDecoder jwtDecoder;

    public CustomAuthenticationManagerResolver(CustomOAuth2AuthorizationService authorizationService, JwtDecoder jwtDecoder) {
        this.authorizationService = authorizationService;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest context) {
        return jwtAuthenticationManager();
    }

    private AuthenticationManager jwtAuthenticationManager() {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(new CustomJwtAuthenticationConverter(authorizationService));
        return provider::authenticate;
    }
}
