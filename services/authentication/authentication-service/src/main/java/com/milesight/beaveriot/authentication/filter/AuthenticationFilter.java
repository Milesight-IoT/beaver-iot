package com.milesight.beaveriot.authentication.filter;

import com.milesight.beaveriot.authentication.service.UserAuthenticationService;
import com.milesight.beaveriot.authentication.exception.CustomOAuth2Exception;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author loong
 * @date 2024/10/17 9:25
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    @Lazy
    @Autowired
    UserAuthenticationService userAuthenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            userAuthenticationService.loadSecurityContext(request);
        } catch (Exception e) {
            CustomOAuth2Exception.exceptionResponse(response, e);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
