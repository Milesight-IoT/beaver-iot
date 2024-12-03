package com.milesight.beaveriot.base.tracer;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author leon
 */
public interface TraceIdProvider {

    String TRACE_ID = "traceId";

    String getTraceId();

    String generateTraceId();

    void clearTraceId();

    ServiceLoader<TraceIdProvider> traceIdProviders = ServiceLoader.load(TraceIdProvider.class);

    static TraceIdProvider traceIdProvider() {
        Optional<TraceIdProvider> traceIdProviderOptional = traceIdProviders.findFirst();
        return traceIdProviderOptional.orElse(new DefaultTraceIdProvider());
    }

}
