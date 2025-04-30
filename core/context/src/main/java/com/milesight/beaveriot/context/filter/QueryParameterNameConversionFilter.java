package com.milesight.beaveriot.context.filter;

import com.google.common.base.CaseFormat;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryParameterNameConversionFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/workflow-http-in");
    }

    @Override
    @SneakyThrows
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) {
        Map<String, String[]> params = new ConcurrentHashMap<>();

        for (String paramName : request.getParameterMap().keySet()) {
            params.put(paramName, request.getParameterValues(paramName));
            String camelCaseParamName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, paramName);
            params.computeIfAbsent(camelCaseParamName, k -> request.getParameterValues(paramName));
        }

        filterChain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                return params.containsKey(name) ? params.get(name)[0] : null;
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return Collections.enumeration(params.keySet());
            }

            @Override
            public String[] getParameterValues(String name) {
                return params.get(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return params;
            }
        }, response);
    }

}
