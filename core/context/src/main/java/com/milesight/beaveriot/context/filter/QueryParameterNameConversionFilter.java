package com.milesight.beaveriot.context.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.base.tracer.TraceIdProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryParameterNameConversionFilter extends OncePerRequestFilter {
    private static final String KEYWORDS_FILE_TOO_LARGE_EXCEPTION = "FileTooLargeException";
    private static final String KEYWORDS_REQUEST_TOO_BIG_EXCEPTION = "RequestTooBigException";

    private final ObjectMapper objectMapper;

    public QueryParameterNameConversionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/workflow-http-in");
    }

    @Override
    @SneakyThrows
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) {
        Map<String, String[]> params = new ConcurrentHashMap<>();

        try {
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
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                String causeName = cause.getClass().getName();
                if (causeName.contains(KEYWORDS_FILE_TOO_LARGE_EXCEPTION)) {
                    handleFileTooLargeException(response);
                    return;
                } else if (causeName.contains(KEYWORDS_REQUEST_TOO_BIG_EXCEPTION)) {
                    handleRequestTooBigException(response);
                    return;
                }
            }
            throw e;
        }
    }

    private void handleFileTooLargeException(HttpServletResponse response) throws IOException {
        handleDataTooLargeRelatedException(response, "File size exceeds the maximum limit");
    }

    private void handleRequestTooBigException(HttpServletResponse response) throws IOException {
        handleDataTooLargeRelatedException(response, "Request size exceeds the maximum limit");
    }

    private void handleDataTooLargeRelatedException(HttpServletResponse response, String errorMessage) throws IOException {
        TraceIdProvider traceIdProvider = TraceIdProvider.traceIdProvider();
        if (!StringUtils.hasText(traceIdProvider.getTraceId())) {
            traceIdProvider.generateTraceId();
        }

        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String jsonResponse = objectMapper.writeValueAsString(
                ResponseBuilder.fail(ErrorCode.DATA_TOO_LARGE, errorMessage)
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
