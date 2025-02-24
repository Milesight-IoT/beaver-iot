package com.milesight.beaveriot.context.filter;

import com.milesight.beaveriot.authentication.facade.IAuthenticationFacade;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * @author loong
 */
public class HttpRequestFilter implements Filter {

    private final IAuthenticationFacade authenticationFacade;

    public HttpRequestFilter(IAuthenticationFacade authenticationFacade) {
        this.authenticationFacade = authenticationFacade;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Map<String, Object> user = authenticationFacade.getUserByToken(token);
            if (user == null || user.isEmpty()) {
                throw ServiceException.with(ErrorCode.AUTHENTICATION_FAILED).detailMessage("token is invalid").build();
            }
            String tenantId = user.get(TenantContext.TENANT_ID).toString();
            TenantContext.setTenantId(tenantId);
        }else {
            String tenantId = httpRequest.getParameter("tenantId");
            if (tenantId != null && !tenantId.isEmpty()) {
                TenantContext.setTenantId(tenantId);
            }
        }
        chain.doFilter(request, response);
    }
}
