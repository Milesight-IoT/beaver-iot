package com.milesight.beaveriot.base.tracer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author leon
 */
public class TraceIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestTraceID = request.getHeader(TraceIdProvider.TRACE_ID);
        String traceId = (StringUtils.hasText(requestTraceID)) ? requestTraceID : TraceIdProvider.traceIdProvider().generateTraceId();
        MDC.put(TraceIdProvider.TRACE_ID, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TraceIdProvider.traceIdProvider().clearTraceId();
        MDC.remove(TraceIdProvider.TRACE_ID);
    }
}