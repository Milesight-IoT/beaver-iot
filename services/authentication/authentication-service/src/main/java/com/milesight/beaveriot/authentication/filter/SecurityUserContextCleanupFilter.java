package com.milesight.beaveriot.authentication.filter;

import com.milesight.beaveriot.authentication.exception.CustomOAuth2Exception;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author loong
 * @date 2024/10/17 9:25
 */
public class SecurityUserContextCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            CustomOAuth2Exception.exceptionResponse(response, e);
        } finally {
            SecurityUserContext.clear();
        }
    }

}
