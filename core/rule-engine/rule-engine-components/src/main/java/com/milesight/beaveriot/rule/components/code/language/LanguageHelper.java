package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.support.JsonHelper;
import org.apache.camel.Exchange;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.util.List;
import java.util.Map;

/**
 * LanguageHelper class.
 *
 * @author simon
 * @date 2025/6/9
 */
public class LanguageHelper {
    private LanguageHelper() {}

    public static Context newContext(String lang) {
        ResourceLimits limits = ResourceLimits.newBuilder()
                .statementLimit(1000, null)
                .build();
        Context.Builder contextBuilder = Context.newBuilder(lang)
                .allowHostAccess(HostAccess.newBuilder()
                        .allowListAccess(true)
                        .allowArrayAccess(true)
                        .allowMapAccess(true)
                        .allowPublicAccess(true)
                        .build())
                .allowIO(IOAccess.NONE)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .allowNativeAccess(false)
                .allowHostClassLoading(false)
                .allowCreateProcess(false)
                .allowCreateThread(false)
                .allowValueSharing(false)
                .resourceLimits(limits)
                .option("engine.WarnInterpreterOnly", "false");
        return contextBuilder.build();
    }

    public static Object convertResultValue(Value value, Exchange exchange, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (value.isNumber()) {
            return value.as(Number.class);
        } else if (value.isBoolean()) {
            return value.as(Boolean.class);
        } else {
            Object out = value.as(Object.class);
            if (out instanceof List<?>) {
                return JsonHelper.cast(out, List.class);
            } else if (out instanceof Map) {
                return JsonHelper.cast(out, Map.class);
            } else {
                return exchange.getContext().getTypeConverter().convertTo(type, exchange, out);
            }
        }
    }
}
