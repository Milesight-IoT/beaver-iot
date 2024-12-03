package com.milesight.beaveriot.base.tracer;

import com.milesight.beaveriot.base.constants.StringConstant;

import java.util.UUID;

/**
 * @author leon
 */
public class DefaultTraceIdProvider implements TraceIdProvider {

    @Override
    public String getTraceId() {
        return TraceContext.getTraceId();
    }

    @Override
    public String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace(StringConstant.DASHED, "");
        TraceContext.setTraceId(traceId);
        return traceId;
    }

    @Override
    public void clearTraceId() {
        TraceContext.clear();
    }

    public class TraceContext {
        private static final ThreadLocal<String> traceIDHolder = new ThreadLocal<>();

        private TraceContext() {
        }

        public static void setTraceId(String traceID) {
            traceIDHolder.set(traceID);
        }

        public static String getTraceId() {
            return traceIDHolder.get();
        }

        public static void clear() {
            traceIDHolder.remove();
        }
    }
}
